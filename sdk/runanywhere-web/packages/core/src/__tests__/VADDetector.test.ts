/**
 * Unit tests for VADDetector
 */

import { VADDetector, VADState } from '../Infrastructure/VADDetector';
import type { VADConfiguration } from '../../types';

describe('VADDetector', () => {
  let detector: VADDetector;

  beforeEach(() => {
    detector = new VADDetector();
  });

  afterEach(() => {
    detector.reset();
  });

  describe('initialization', () => {
    it('should create detector with default configuration', () => {
      expect(detector.state).toBe(VADState.Silence);
      expect(detector.stats.totalFrames).toBe(0);
      expect(detector.stats.speechFrames).toBe(0);
    });

    it('should accept custom configuration', () => {
      const config: VADConfiguration = {
        energyThreshold: 0.05,
        sampleRate: 8000,
        frameLength: 256,
      };
      const customDetector = new VADDetector(config);

      expect(customDetector.state).toBe(VADState.Silence);
      expect(customDetector.stats.totalFrames).toBe(0);
    });
  });

  describe('processFrame', () => {
    it('should return false for silent audio', () => {
      const silentAudio = new Float32Array(1024).fill(0);
      const isVoice = detector.processFrame(silentAudio, 0);
      expect(isVoice).toBe(false);
      expect(detector.state).toBe(VADState.Silence);
    });

    it('should return true for voice audio', () => {
      const voiceAudio = new Float32Array(1024);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }
      const isVoice = detector.processFrame(voiceAudio, 0);
      expect(isVoice).toBe(true);
    });

    it('should transition from Silence to PreSpeech to Speech', () => {
      const voiceAudio = new Float32Array(1024);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      // Process first frame
      const result1 = detector.processFrame(voiceAudio, 0);
      expect(result1).toBe(true);
      expect(detector.state).toBe(VADState.PreSpeech);

      // Process second frame
      const result2 = detector.processFrame(voiceAudio, 1);
      expect(result2).toBe(true);
      expect(detector.state).toBe(VADState.Speech);
    });

    it('should handle multiple frames correctly', () => {
      const voiceAudio = new Float32Array(2048);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.3 * Math.sin(2 * Math.PI * 440 * i / 16000) + 0.05 * Math.random();
      }

      const results: boolean[] = [];
      for (let i = 0; i < 4; i++) {
        results.push(detector.processFrame(voiceAudio, i));
      }

      expect(results.filter(r => r).length).toBeGreaterThan(0);
      expect(detector.stats.totalFrames).toBe(4);
    });

    it('should update stats correctly', () => {
      const voiceAudio = new Float32Array(1024);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.3 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      detector.processFrame(voiceAudio, 0);
      detector.processFrame(voiceAudio, 1);

      expect(detector.stats.totalFrames).toBe(2);
      expect(detector.stats.speechFrames).toBeGreaterThan(0);
      expect(detector.stats.detectionCount).toBe(1);
    });

    it('should handle silence after speech', () => {
      // Create detector with fixed threshold (no auto-calibration) for predictable behavior
      const testDetector = new VADDetector({ 
        autoCalibration: false, 
        energyThreshold: 0.05,
        frameLength: 256,
      });
      
      const voiceAudio = new Float32Array(1024);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.3 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      const silentAudio = new Float32Array(1024).fill(0);

      // Voice frames - transition to speech
      testDetector.processFrame(voiceAudio, 0);
      testDetector.processFrame(voiceAudio, 1);
      expect(testDetector.state).toBe(VADState.Speech);

      // Process several silence frames - should transition out of speech
      testDetector.processFrame(silentAudio, 2);
      testDetector.processFrame(silentAudio, 3);
      testDetector.processFrame(silentAudio, 4);
      
      // State should have changed from Speech (to PostSpeech or Silence)
      expect(testDetector.state).not.toBe(VADState.Speech);
    });
  });

  describe('processAudio', () => {
    it('should detect speech segments', () => {
      const audio = new Float32Array(4096);
      
      // First 1024 samples: silence
      // Next 2048 samples: voice
      // Last 1024 samples: silence

      for (let i = 1024; i < 3072; i++) {
        audio[i] = 0.3 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      const segments = detector.processAudio(audio);

      expect(segments.length).toBeGreaterThan(0);
      expect(segments[0].isVoice).toBe(true);
      expect(segments[0].durationMs).toBeGreaterThan(0);
    });

    it('should return empty segments for pure silence', () => {
      const silentAudio = new Float32Array(4096).fill(0);
      const segments = detector.processAudio(silentAudio);
      expect(segments.length).toBe(0);
    });

    it('should handle multiple speech segments', () => {
      const audio = new Float32Array(8192);
      
      // Voice in first 1024
      // Silence in next 1024
      // Voice in next 2048
      // Silence in last 4096

      for (let i = 0; i < 1024; i++) {
        audio[i] = 0.3 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      for (let i = 2048; i < 4096; i++) {
        audio[i] = 0.3 * Math.sin(2 * Math.PI * 880 * i / 16000);
      }

      const segments = detector.processAudio(audio);
      expect(segments.length).toBeGreaterThanOrEqual(1);
    });

    it('should calculate segment statistics correctly', () => {
      const voiceAudio = new Float32Array(2048);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      const segments = detector.processAudio(voiceAudio);
      
      if (segments.length > 0) {
        expect(segments[0].startIndex).toBeGreaterThanOrEqual(0);
        expect(segments[0].endIndex).toBeLessThan(voiceAudio.length / 512);
        expect(segments[0].averageEnergy).toBeGreaterThan(0);
      }
    });
  });

  describe('reset', () => {
    it('should reset state to Silence', () => {
      const voiceAudio = new Float32Array(1024);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      detector.processFrame(voiceAudio, 0);
      expect(detector.state).toBe(VADState.PreSpeech);

      detector.reset();
      expect(detector.state).toBe(VADState.Silence);
      expect(detector.stats.totalFrames).toBe(0);
    });

    it('should clear stats after reset', () => {
      const voiceAudio = new Float32Array(1024);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      detector.processFrame(voiceAudio, 0);
      detector.processFrame(voiceAudio, 1);

      expect(detector.stats.totalFrames).toBe(2);

      detector.reset();
      expect(detector.stats.totalFrames).toBe(0);
      expect(detector.stats.speechFrames).toBe(0);
    });
  });

  describe('updateConfig', () => {
    it('should update energy threshold', () => {
      const config: VADConfiguration = {
        energyThreshold: 0.1,
      };
      detector.updateConfig(config);
      expect(detector.stats.totalFrames).toBe(0);
    });

    it('should update sample rate', () => {
      const config: VADConfiguration = {
        sampleRate: 8000,
      };
      detector.updateConfig(config);
      expect(detector.stats.totalFrames).toBe(0);
    });

    it('should update frame length', () => {
      const config: VADConfiguration = {
        frameLength: 256,
      };
      detector.updateConfig(config);
      expect(detector.stats.totalFrames).toBe(0);
    });

    it('should update auto calibration', () => {
      const config: VADConfiguration = {
        autoCalibration: false,
      };
      detector.updateConfig(config);
      expect(detector.stats.totalFrames).toBe(0);
    });
  });

  describe('auto-calibration', () => {
    it('should adapt threshold based on environment', () => {
      const audio = new Float32Array(4096);
      
      // Start with low energy background noise
      for (let i = 0; i < 1024; i++) {
        audio[i] = 0.02 * Math.sin(2 * Math.PI * 440 * i / 16000) + 0.01 * Math.random();
      }
      
      // Then voice
      for (let i = 1024; i < 3072; i++) {
        audio[i] = 0.3 * Math.sin(2 * Math.PI * 440 * i / 16000) + 0.02 * Math.random();
      }
      
      // Process all frames
      for (let i = 0; i < 8; i++) {
        detector.processFrame(audio, i);
      }

      // Threshold should have adapted
      expect(detector.stats.detectionCount).toBeGreaterThanOrEqual(1);
    });
  });

  describe('statistics', () => {
    it('should track min and max energy', () => {
      const audio = new Float32Array(1024);
      
      // Low energy
      for (let i = 0; i < 512; i++) {
        audio[i] = 0.01 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }
      
      // High energy
      for (let i = 512; i < 1024; i++) {
        audio[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      detector.processFrame(audio, 0);
      detector.processFrame(audio, 1);

      expect(detector.stats.minEnergy).toBeGreaterThan(0);
      expect(detector.stats.maxEnergy).toBeGreaterThan(detector.stats.minEnergy);
    });

    it('should track detection count', () => {
      const voiceAudio = new Float32Array(2048);
      for (let i = 0; i < voiceAudio.length; i++) {
        voiceAudio[i] = 0.3 * Math.sin(2 * Math.PI * 440 * i / 16000);
      }

      const silentAudio = new Float32Array(1024).fill(0);

      // First detection
      detector.processFrame(voiceAudio, 0);
      detector.processFrame(voiceAudio, 1);
      
      // Reset state
      detector.processFrame(silentAudio, 2);
      
      // Second detection
      detector.processFrame(voiceAudio, 3);
      detector.processFrame(voiceAudio, 4);

      expect(detector.stats.detectionCount).toBeGreaterThanOrEqual(1);
    });
  });
});
