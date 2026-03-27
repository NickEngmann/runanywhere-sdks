/**
 * RunAnywhere Web SDK - Voice Activity Detection (VAD)
 *
 * Implements energy-based voice activity detection for determining
 * when speech starts and stops in an audio stream.
 */

import { SDKLogger } from '../Foundation/SDKLogger';
import type { VADConfiguration } from '../../types';

const logger = new SDKLogger('VAD');

/**
 * VAD detector states
 */
export enum VADState {
  Silence = 'silence',
  PreSpeech = 'preSpeech',
  Speech = 'speech',
  PostSpeech = 'postSpeech',
}

/**
 * VAD statistics for monitoring detection quality
 */
export interface VADStats {
  totalFrames: number;
  speechFrames: number;
  silenceFrames: number;
  detectionCount: number;
  averageEnergy: number;
  minEnergy: number;
  maxEnergy: number;
}

/**
 * Speech segment detected by VAD
 */
export interface SpeechSegment {
  startIndex: number;
  endIndex: number;
  durationMs: number;
  averageEnergy: number;
  isVoice: boolean;
}

/**
 * Voice Activity Detector using energy-based threshold
 */
export class VADDetector {
  private _state: VADState = VADState.Silence;
  private _config: Required<VADConfiguration>;
  private _buffer: number[] = [];
  private _stats: VADStats = {
    totalFrames: 0,
    speechFrames: 0,
    silenceFrames: 0,
    detectionCount: 0,
    averageEnergy: 0,
    minEnergy: Infinity,
    maxEnergy: -Infinity,
  };
  private _energyHistory: number[] = [];
  private _currentSegmentStart: number | null = null;
  private _sampleRate: number;
  private _frameDuration: number;

  constructor(config: VADConfiguration = {}) {
    this._config = {
      energyThreshold: config.energyThreshold ?? 0.01,
      sampleRate: config.sampleRate ?? 16000,
      frameLength: config.frameLength ?? 512,
      autoCalibration: config.autoCalibration ?? true,
    };
    this._sampleRate = this._config.sampleRate;
    this._frameDuration = (this._config.frameLength / this._sampleRate) * 1000; // ms
  }

  /**
   * Get current VAD state
   */
  get state(): VADState {
    return this._state;
  }

  /**
   * Get current statistics
   */
  get stats(): VADStats {
    return { ...this._stats };
  }

  /**
   * Process audio frame and detect voice activity
   * @param audioData Audio samples (Float32Array)
   * @param frameIndex Current frame index in the audio stream
   * @returns true if voice is detected, false otherwise
   */
  processFrame(audioData: Float32Array, frameIndex: number): boolean {
    // Extract frame from audio data
    const frameLength = this._config.frameLength;
    const startIdx = frameIndex * frameLength;
    const frame = audioData.slice(startIdx, startIdx + frameLength);

    if (frame.length === 0) {
      return false;
    }

    // Calculate frame energy (RMS)
    let sumSquared = 0;
    for (let i = 0; i < frame.length; i++) {
      sumSquared += frame[i] * frame[i];
    }
    const energy = Math.sqrt(sumSquared / frame.length);

    // Update energy history for auto-calibration
    this._energyHistory.push(energy);
    if (this._energyHistory.length > 100) {
      this._energyHistory.shift();
    }

    // Auto-calibrate threshold based on recent energy
    if (this._config.autoCalibration && this._energyHistory.length >= 10) {
      const avgEnergy = this._energyHistory.reduce((a, b) => a + b, 0) / this._energyHistory.length;
      this._config.energyThreshold = Math.max(avgEnergy * 0.5, 0.001);
    }

    // Update stats
    this._stats.totalFrames++;
    this._stats.speechFrames += energy > this._config.energyThreshold ? 1 : 0;
    this._stats.silenceFrames += energy <= this._config.energyThreshold ? 1 : 0;
    this._stats.detectionCount += energy > this._config.energyThreshold && this._state === VADState.Silence ? 1 : 0;
    this._stats.averageEnergy = (this._stats.averageEnergy * (this._stats.totalFrames - 1) + energy) / this._stats.totalFrames;
    this._stats.minEnergy = Math.min(this._stats.minEnergy, energy);
    this._stats.maxEnergy = Math.max(this._stats.maxEnergy, energy);

    // State machine
    const isVoice = energy > this._config.energyThreshold;

    switch (this._state) {
      case VADState.Silence:
        if (isVoice) {
          this._state = VADState.PreSpeech;
          this._currentSegmentStart = frameIndex;
          logger.debug(`Voice detected at frame ${frameIndex}, energy: ${energy.toFixed(4)}, threshold: ${this._config.energyThreshold.toFixed(4)}`);
        }
        break;

      case VADState.PreSpeech:
        if (isVoice) {
          this._state = VADState.Speech;
        } else {
          this._state = VADState.Silence;
          this._currentSegmentStart = null;
        }
        break;

      case VADState.Speech:
        if (!isVoice) {
          this._state = VADState.PostSpeech;
          logger.debug(`Voice ended at frame ${frameIndex}, duration: ${(frameIndex - (this._currentSegmentStart ?? 0)) * this._frameDuration}ms`);
        }
        break;

      case VADState.PostSpeech:
        if (isVoice) {
          this._state = VADState.Speech;
        } else {
          // End of speech segment
          const segmentEnd = frameIndex;
          const durationMs = (segmentEnd - (this._currentSegmentStart ?? 0)) * this._frameDuration;
          logger.info(`Speech segment complete: ${durationMs.toFixed(0)}ms`);
          this._state = VADState.Silence;
          this._currentSegmentStart = null;
        }
        break;
    }

    return isVoice;
  }

  /**
   * Process entire audio buffer in frames
   * @param audioData Full audio data
   * @returns Array of speech segments detected
   */
  processAudio(audioData: Float32Array): SpeechSegment[] {
    const frameLength = this._config.frameLength;
    const numFrames = Math.ceil(audioData.length / frameLength);
    const segments: SpeechSegment[] = [];

    for (let i = 0; i < numFrames; i++) {
      const isVoice = this.processFrame(audioData, i);

      // If voice detected and segment not started, mark start
      if (isVoice && this._currentSegmentStart === null && this._state === VADState.PreSpeech) {
        this._currentSegmentStart = i;
      }

      // If voice ended and segment was active, record segment
      if (!isVoice && this._state === VADState.PostSpeech && this._currentSegmentStart !== null) {
        const segment: SpeechSegment = {
          startIndex: this._currentSegmentStart,
          endIndex: i,
          durationMs: (i - this._currentSegmentStart) * this._frameDuration,
          averageEnergy: this._calculateSegmentEnergy(audioData, this._currentSegmentStart, i, frameLength),
          isVoice: true,
        };
        segments.push(segment);
        this._currentSegmentStart = null;
      }
    }

    // Handle case where audio ends during speech
    if (this._currentSegmentStart !== null && this._state === VADState.Speech) {
      const segment: SpeechSegment = {
        startIndex: this._currentSegmentStart,
        endIndex: numFrames - 1,
        durationMs: (numFrames - 1 - this._currentSegmentStart) * this._frameDuration,
        averageEnergy: this._calculateSegmentEnergy(audioData, this._currentSegmentStart, numFrames - 1, frameLength),
        isVoice: true,
      };
      segments.push(segment);
    }

    return segments;
  }

  /**
   * Calculate average energy for a segment
   */
  private _calculateSegmentEnergy(
    audioData: Float32Array,
    startIndex: number,
    endIndex: number,
    frameLength: number
  ): number {
    let sumSquared = 0;
    let count = 0;

    for (let i = startIndex; i <= endIndex; i++) {
      const startIdx = i * frameLength;
      const frame = audioData.slice(startIdx, startIdx + frameLength);
      for (let j = 0; j < frame.length; j++) {
        sumSquared += frame[j] * frame[j];
        count++;
      }
    }

    return count > 0 ? Math.sqrt(sumSquared / count) : 0;
  }

  /**
   * Reset the VAD detector to initial state
   */
  reset(): void {
    this._state = VADState.Silence;
    this._buffer = [];
    this._currentSegmentStart = null;
    this._energyHistory = [];
    this._stats = {
      totalFrames: 0,
      speechFrames: 0,
      silenceFrames: 0,
      detectionCount: 0,
      averageEnergy: 0,
      minEnergy: Infinity,
      maxEnergy: -Infinity,
    };
    logger.debug('VAD detector reset');
  }

  /**
   * Update configuration
   */
  updateConfig(config: VADConfiguration): void {
    if (config.energyThreshold !== undefined) {
      this._config.energyThreshold = config.energyThreshold;
    }
    if (config.sampleRate !== undefined) {
      this._config.sampleRate = config.sampleRate;
      this._frameDuration = (this._config.frameLength / this._config.sampleRate) * 1000;
    }
    if (config.frameLength !== undefined) {
      this._config.frameLength = config.frameLength;
      this._frameDuration = (this._config.frameLength / this._config.sampleRate) * 1000;
    }
    if (config.autoCalibration !== undefined) {
      this._config.autoCalibration = config.autoCalibration;
    }
    logger.debug(`VAD configuration updated: ${JSON.stringify(this._config)}`);
  }
}
