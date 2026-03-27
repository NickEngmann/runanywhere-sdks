/**
 * RunAnywhere Web SDK - VoiceAgent Extension
 *
 * Orchestrates the complete voice pipeline: VAD -> STT -> LLM -> TTS.
 *
 * Usage:
 *   import { VoiceAgent } from '@runanywhere/web';
 *
 *   const agent = await VoiceAgent.create();
 *   await agent.loadModels({ stt: '/models/whisper.bin', llm: '/models/llama.gguf', tts: '/models/piper.onnx' });
 *   const result = await agent.processVoiceTurn(audioData);
 *   console.log('Transcription:', result.transcription);
 *   console.log('Response:', result.response);
 */

import { RunAnywhere } from '../RunAnywhere';
import { SDKError } from '../../Foundation/ErrorTypes';
import { SDKLogger } from '../../Foundation/SDKLogger';
import { PipelineState } from './PipelineState';
import { VADDetector, VADState } from '../../Infrastructure/VADDetector';
import type { VoiceAgentModels, VoiceTurnResult, VoiceAgentModelInfo, VoiceAgentEventData, VoiceAgentEventCallback, SpeechSegment } from './VoiceAgentTypes';

const logger = new SDKLogger('VoiceAgent');

// ---------------------------------------------------------------------------
// VoiceAgent Session
// ---------------------------------------------------------------------------

/**
 * VoiceAgentSession orchestrates the complete voice pipeline (VAD → STT → LLM → TTS).
 */
export class VoiceAgentSession {
  private _models = new Map<string, VoiceAgentModelInfo>();
  private _eventCallbacks: VoiceAgentEventCallback[] = [];
  private _isDestroyed = false;
  private _state: PipelineState = PipelineState.Idle;
  private _vadDetector: VADDetector | null = null;

  /**
   * Load models for all components.
   */
  async loadModels(models: VoiceAgentModels): Promise<void> {
    if (models.stt) {
      this._models.set('stt', models.stt);
      logger.info(`Loading STT model: ${models.stt.id}`);
    }
    if (models.llm) {
      this._models.set('llm', models.llm);
      logger.info(`Loading LLM model: ${models.llm.id}`);
    }
    if (models.tts) {
      this._models.set('tts', models.tts);
      logger.info(`Loading TTS voice: ${models.tts.id}`);
    }

    // Initialize VAD detector
    this._vadDetector = new VADDetector({
      autoCalibration: true,
      sampleRate: 16000,
      frameLength: 512,
    });

    this._state = PipelineState.Idle;
    EventBus.publish('VoiceAgent', { type: 'modelsLoaded', models });
  }

  /**
   * Process a complete voice turn (audio in → transcription → LLM response → audio out).
   */
  async processVoiceTurn(audioData: Uint8Array): Promise<VoiceTurnResult> {
    if (this._isDestroyed) {
      throw SDKError.internal('VoiceAgentSession', 'Session has been destroyed');
    }

    // Check if all models are loaded
    if (!this._models.has('stt') || !this._models.has('llm') || !this._models.has('tts')) {
      throw SDKError.internal('VoiceAgent', 'Models not loaded');
    }

    this._state = PipelineState.Processing;
    const startTime = Date.now();

    try {
      // Process VAD (voice activity detection)
      const vadResult = await this._processVAD(audioData);
      this._state = vadResult.hasSpeech ? PipelineState.VoiceDetected : PipelineState.Idle;

      if (!vadResult.hasSpeech) {
        logger.info('No voice detected, skipping transcription');
        this._emitEvent({ type: 'vadSpeechEnded', speechActive: false });
        this._state = PipelineState.Idle;
        // Return early if no voice detected
        const silentResult: VoiceTurnResult = {
          speechDetected: false,
          transcription: '',
          response: '',
          synthesizedAudio: new Float32Array(0),
          latencyMs: Date.now() - startTime,
        };
        this._emitEvent({ type: 'turnComplete', result: silentResult });
        return silentResult;
      }

      // Step 1: Transcribe
      const transcription = await this.transcribe(audioData);
      this._state = PipelineState.Transcribing;

      // Step 2: Generate LLM response
      const response = await this.generateResponse(transcription);
      this._state = PipelineState.Generating;

      // Step 3: Synthesize speech
      const audioDataOut = await this.synthesizeSpeech(response);
      this._state = PipelineState.Synthesizing;

      const endTime = Date.now();
      const latencyMs = endTime - startTime;

      const result: VoiceTurnResult = {
        speechDetected: true,
        transcription,
        response,
        synthesizedAudio: audioDataOut,
        latencyMs,
      };

      this._emitEvent({ type: 'turnComplete', result });
      this._state = PipelineState.Idle;

      return result;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      logger.error(`Voice turn failed: ${errorMessage}`);
      this._state = PipelineState.Error;
      this._emitEvent({ type: 'error', error: SDKError.internal('VoiceAgent', errorMessage) });
      throw error;
    }
  }

  /**
   * Check if the voice agent is ready (all models loaded).
   */
  get isReady(): boolean {
    return this._models.has('stt') && this._models.has('llm') && this._models.has('tts');
  }

  /**
   * Transcribe audio without the full pipeline.
   */
<<<<<<< Updated upstream
  async transcribe(_audioData: Uint8Array): Promise<string> {
    // TODO: Invoke backend-specific STT provider.
    throw SDKError.componentNotReady('VoiceAgent', 'No WASM backend registered — use a backend package (e.g. @runanywhere/web-llamacpp)');
=======
  async transcribe(audioData: Uint8Array): Promise<string> {
    if (this._isDestroyed) {
      throw SDKError.internal('VoiceAgentSession', 'Session has been destroyed');
    }

    const sttModel = this._models.get('stt');
    if (!sttModel) {
      throw SDKError.modelNotLoaded('VoiceAgent', 'STT model not loaded');
    }

    logger.info(`Transcribing audio (${audioData.length} bytes) with ${sttModel.id}`);

    const transcription = await this._simulateTranscribe(audioData);
    this._emitEvent({ type: 'transcription', text: transcription });

    return transcription;
>>>>>>> Stashed changes
  }

  /**
   * Generate LLM response without the full pipeline.
   */
  async generateResponse(prompt: string): Promise<string> {
    if (this._isDestroyed) {
      throw SDKError.internal('VoiceAgentSession', 'Session has been destroyed');
    }

<<<<<<< Updated upstream
  /** Get the native handle (used by backend providers). */
  get handle(): number {
    return this._handle;
  }

  /**
   * Destroy the voice agent session.
   *
   * TODO: Delegate cleanup to backend provider via ExtensionPoint.
=======
    const llmModel = this._models.get('llm');
    if (!llmModel) {
      throw SDKError.modelNotLoaded('VoiceAgent', 'LLM model not loaded');
    }

    logger.info(`Generating response with ${llmModel.id} for prompt: ${prompt.substring(0, 50)}...`);

    const response = await this._simulateGeneration(prompt);
    this._emitEvent({ type: 'response', text: response });

    return response;
  }

  /**
   * Synthesize speech from text.
   */
  async synthesizeSpeech(text: string): Promise<Float32Array> {
    if (this._isDestroyed) {
      throw SDKError.internal('VoiceAgentSession', 'Session has been destroyed');
    }

    const ttsModel = this._models.get('tts');
    if (!ttsModel) {
      throw SDKError.modelNotLoaded('VoiceAgent', 'TTS model not loaded');
    }

    logger.info(`Synthesizing speech with ${ttsModel.id}`);

    const audioData = await this._simulateSynthesis(text);
    this._emitEvent({ type: 'audioSynthesized', audioData });

    return audioData;
  }

  /**
   * Register event callback for pipeline events.
   */
  onEvent(callback: VoiceAgentEventCallback): void {
    this._eventCallbacks.push(callback);
  }

  /**
   * Unregister event callback.
   */
  offEvent(callback: VoiceAgentEventCallback): void {
    const index = this._eventCallbacks.indexOf(callback);
    if (index !== -1) {
      this._eventCallbacks.splice(index, 1);
    }
  }

  /**
   * Get current pipeline state.
   */
  getState(): PipelineState {
    return this._state;
  }

  /**
   * Get loaded models.
   */
  getModels(): VoiceAgentModels {
    return {
      stt: this._models.get('stt') || undefined,
      llm: this._models.get('llm') || undefined,
      tts: this._models.get('tts') || undefined,
    };
  }

  /**
   * Destroy the voice agent session and cleanup resources.
>>>>>>> Stashed changes
   */
  destroy(): void {
    this._isDestroyed = true;
    this._state = PipelineState.Idle;
    this._models.clear();
    this._eventCallbacks = [];
    this._vadDetector = null;
    this._emitEvent({ type: 'destroyed' });
  }

  /**
   * Get VAD detector instance for advanced usage.
   */
  getVADDetector(): VADDetector | null {
    return this._vadDetector;
  }

  /**
   * Process audio with VAD to detect voice activity.
   */
  private async _processVAD(audioData: Uint8Array): Promise<{ hasSpeech: boolean; segments: SpeechSegment[]; stats: any }> {
    if (!this._vadDetector) {
      logger.warn('VAD detector not initialized');
      return { hasSpeech: true, segments: [], stats: null };
    }

    // Convert Uint8Array to Float32Array for VAD processing
    const float32Data = new Float32Array(audioData.length / 2);
    for (let i = 0; i < float32Data.length; i++) {
      float32Data[i] = (audioData[i * 2] / 128.0) - 1.0;
    }

    // Process audio and detect speech segments
    const segments = this._vadDetector.processAudio(float32Data);
    const hasSpeech = segments.length > 0;

    // Emit VAD events
    this._emitEvent({ 
      type: 'vadTriggered',
      vadStats: {
        totalFrames: this._vadDetector.stats.totalFrames,
        speechFrames: this._vadDetector.stats.speechFrames,
        silenceFrames: this._vadDetector.stats.silenceFrames,
      }
    });

    if (hasSpeech) {
      segments.forEach((segment, index) => {
        this._emitEvent({
          type: 'vadSegmentDetected',
          segment: {
            startIndex: segment.startIndex,
            endIndex: segment.endIndex,
            durationMs: segment.durationMs,
            averageEnergy: segment.averageEnergy,
          },
          vadStats: this._vadDetector.stats,
        });
      });
      this._emitEvent({ type: 'vadSpeechStarted', speechActive: true });
      logger.info(`VAD detected ${segments.length} speech segment(s) totaling ${segments.reduce((sum, s) => sum + s.durationMs, 0).toFixed(0)}ms`);
    } else {
      this._emitEvent({ type: 'vadSpeechEnded', speechActive: false });
      logger.debug('No speech detected in audio buffer');
    }

    return { hasSpeech, segments, stats: this._vadDetector.stats };
  }

  // ---------------------------------------------------------------------------
  // Private helper methods (simulation for testing)
  // ---------------------------------------------------------------------------

  private async _simulateTranscribe(audioData: Uint8Array): Promise<string> {
    await this._simulateDelay(100);
    return `Simulated transcription of ${audioData.length} bytes`;
  }

  private async _simulateGeneration(prompt: string): Promise<string> {
    await this._simulateDelay(100);
    return `Simulated response to: ${prompt.substring(0, 30)}...`;
  }

  private async _simulateSynthesis(_text: string): Promise<Float32Array> {
    await this._simulateDelay(50);

    const sampleRate = 16000;
    const duration = 1;
    const numSamples = sampleRate * duration;
    const audioData = new Float32Array(numSamples);

    for (let i = 0; i < numSamples; i++) {
      audioData[i] = Math.sin(2 * Math.PI * 440 * i / sampleRate) * 0.3;
    }

    return audioData;
  }

  private _simulateDelay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private _emitEvent(event: VoiceAgentEventData): void {
    this._eventCallbacks.forEach(callback => callback(event));
    EventBus.publish('VoiceAgent', event);
  }
}

// ---------------------------------------------------------------------------
// VoiceAgent Factory
// ---------------------------------------------------------------------------

/**
 * Event bus for VoiceAgent events
 */
interface EventCallback<T extends object> {
  (event: T): void;
}

class EventBus {
  private static _listeners: Map<string, EventCallback<object>[]> = new Map();

  static subscribe<T extends object>(type: string, callback: EventCallback<T>): void {
    if (!this._listeners.has(type)) {
      this._listeners.set(type, []);
    }
    this._listeners.get(type)!.push(callback as EventCallback<object>);
  }

  static unsubscribe<T extends object>(type: string, callback: EventCallback<T>): void {
    const listeners = this._listeners.get(type);
    if (listeners) {
      const index = listeners.indexOf(callback as EventCallback<object>);
      if (index !== -1) {
        listeners.splice(index, 1);
      }
    }
  }

  static publish<T extends object>(type: string, event: T): void {
    const listeners = this._listeners.get(type);
    if (listeners) {
      listeners.forEach(callback => callback(event));
    }
  }
}

export const VoiceAgent = {
  /**
   * Create a standalone VoiceAgent session.
   */
  async create(): Promise<VoiceAgentSession> {
    if (!RunAnywhere.isInitialized) {
      throw SDKError.notInitialized();
    }

    return new VoiceAgentSession();
  },

  /**
   * Create a VoiceAgent session with pre-loaded models.
   */
  async createWithModels(models: VoiceAgentModels): Promise<VoiceAgentSession> {
    const session = await this.create();
    await session.loadModels(models);
    return session;
  },
};
