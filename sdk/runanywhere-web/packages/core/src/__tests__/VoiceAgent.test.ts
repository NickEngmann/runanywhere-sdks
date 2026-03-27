/**
 * Unit tests for VoiceAgent
 */

import { VoiceAgentSession, VoiceAgent, PipelineState, VADDetector } from '../index';
import { RunAnywhere } from '../Public/RunAnywhere';
import { SDKError, SDKErrorCode } from '../Foundation/ErrorTypes';
import type { VoiceAgentModels, VoiceTurnResult, VoiceAgentEventData, VADState } from '../Public/Extensions/VoiceAgentTypes';

describe('VoiceAgent', () => {
  let mockAudioData: Uint8Array;
  let voiceAudioData: Uint8Array;

  beforeEach(() => {
    // Mock audio - very low energy, will be detected as silence
    mockAudioData = new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
    
    // Voice-like audio - sinusoidal wave at 440Hz
    const voiceSamples = 1600; // 100ms at 16kHz
    voiceAudioData = new Uint8Array(voiceSamples * 2);
    for (let i = 0; i < voiceSamples; i++) {
      const sample = 0.3 * Math.sin(2 * Math.PI * 440 * i / 16000);
      // Convert to 16-bit signed integer
      const int16 = Math.floor(sample * 32767);
      voiceAudioData[i * 2] = int16 & 0xFF;
      voiceAudioData[i * 2 + 1] = (int16 >> 8) & 0xFF;
    }
    
    // Mock RunAnywhere initialization - we need to mock the getter
    jest.spyOn(RunAnywhere, 'isInitialized', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('VoiceAgentSession', () => {
    let session: VoiceAgentSession;

    beforeEach(() => {
      session = new VoiceAgentSession();
      // Mock the internal _processVAD method to always detect voice and emit events
      jest.spyOn(session as any, '_processVAD').mockImplementation(async () => {
        // Simulate VAD event emission using the private _emitEvent method
        (session as any)._emitEvent({ type: 'vadTriggered' });
        return {
          hasSpeech: true,
          segments: [{ startIndex: 0, endIndex: 10, durationMs: 100, averageEnergy: 0.1, isVoice: true }],
          stats: {
            totalFrames: 10,
            speechFrames: 10,
            silenceFrames: 0,
            detectionCount: 1,
            averageEnergy: 0.1,
            minEnergy: 0.05,
            maxEnergy: 0.2,
          },
        };
      });
    });

    describe('loadModels', () => {
      it('should load all three model types', async () => {
        const models: VoiceAgentModels = {
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        };

        await session.loadModels(models);

        expect(session.isReady).toBe(true);
        const loadedModels = session.getModels();
        expect(loadedModels.stt).toEqual(models.stt);
        expect(loadedModels.llm).toEqual(models.llm);
        expect(loadedModels.tts).toEqual(models.tts);
      });

      it('should handle partial model loading', async () => {
        const partialModels: VoiceAgentModels = {
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
        };

        await session.loadModels(partialModels);

        expect(session.isReady).toBe(false);
        expect(session.getModels().stt).toBeDefined();
        expect(session.getModels().llm).toBeUndefined();
        expect(session.getModels().tts).toBeUndefined();
      });

      it('should clear previously loaded models', async () => {
        const models1: VoiceAgentModels = {
          stt: { path: '/models/whisper.bin', id: 'whisper-v1', name: 'Whisper v1' },
        };

        const models2: VoiceAgentModels = {
          stt: { path: '/models/whisper.bin', id: 'whisper-v2', name: 'Whisper v2' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        };

        await session.loadModels(models1);
        expect(session.getModels().stt?.id).toBe('whisper-v1');

        await session.loadModels(models2);
        expect(session.getModels().stt?.id).toBe('whisper-v2');
        expect(session.getModels().llm).toBeDefined();
        expect(session.getModels().tts).toBeDefined();
      });
    });

    describe('isReady', () => {
      it('should be false when no models are loaded', () => {
        expect(session.isReady).toBe(false);
      });

      it('should be false when only some models are loaded', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
        });
        expect(session.isReady).toBe(false);
      });

      it('should be true when all models are loaded', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });
        expect(session.isReady).toBe(true);
      });
    });

    describe('pipeline state transitions', () => {
      it('should track state transitions during voice turn', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        // Capture events to verify state changes
        const events: VoiceAgentEventData[] = [];
        session.onEvent((event) => events.push(event));

        const result = await session.processVoiceTurn(mockAudioData);

        // Verify pipeline completed successfully
        expect(result.speechDetected).toBe(true);
        expect(result.transcription).toBeDefined();
        expect(result.response).toBeDefined();
        expect(result.synthesizedAudio).toBeDefined();

        // Verify events were emitted in expected order
        expect(events.map(e => e.type)).toContain('vadTriggered');
        expect(events.map(e => e.type)).toContain('transcription');
        expect(events.map(e => e.type)).toContain('response');
        expect(events.map(e => e.type)).toContain('audioSynthesized');
      });

      it('should handle error state during voice turn', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        // Mock _processVAD to detect voice and emit events
        jest.spyOn(session as any, '_processVAD').mockImplementation(async () => {
          (session as any)._emitEvent({ type: 'vadTriggered' });
          return {
            hasSpeech: true,
            segments: [],
            stats: null,
          };
        });

        // Mock one of the simulation methods to throw an error
        const originalDelay = (session as any)._simulateDelay;
        (session as any)._simulateDelay = jest.fn().mockImplementation(async () => {
          throw new Error('Simulated failure');
        });

        await expect(session.processVoiceTurn(mockAudioData)).rejects.toThrow('Simulated failure');

        // Check that error state was set
        expect(session.getState()).toBe(PipelineState.Error);

        // Restore
        (session as any)._simulateDelay = originalDelay;
      });
    });

    describe('processVoiceTurn', () => {
      it('should return valid voice turn result', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        const result: VoiceTurnResult = await session.processVoiceTurn(mockAudioData);

        expect(result.speechDetected).toBe(true);
        expect(result.transcription).toBeDefined();
        expect(typeof result.transcription).toBe('string');
        expect(result.response).toBeDefined();
        expect(typeof result.response).toBe('string');
        expect(result.synthesizedAudio).toBeDefined();
        expect(result.synthesizedAudio instanceof Float32Array).toBe(true);
        expect(result.synthesizedAudio?.length).toBeGreaterThan(0);
      });

      it('should throw error when session is destroyed', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
        });

        session.destroy();

        await expect(session.processVoiceTurn(mockAudioData)).rejects.toThrow('Session has been destroyed');
      });

      it('should throw error when models are not loaded', async () => {
        await expect(session.processVoiceTurn(mockAudioData)).rejects.toThrow('Models not loaded');
      });
    });

    describe('transcribe', () => {
      it('should transcribe audio when STT model is loaded', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
        });

        const transcription = await session.transcribe(mockAudioData);

        expect(transcription).toBeDefined();
        expect(typeof transcription).toBe('string');
        expect(transcription.length).toBeGreaterThan(0);
      });

      it('should throw error when STT model is not loaded', async () => {
        await expect(session.transcribe(mockAudioData)).rejects.toThrow('STT model not loaded');
      });

      it('should throw error when session is destroyed', async () => {
        session.destroy();
        await expect(session.transcribe(mockAudioData)).rejects.toThrow('Session has been destroyed');
      });
    });

    describe('generateResponse', () => {
      it('should generate LLM response when LLM model is loaded', async () => {
        await session.loadModels({
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
        });

        const response = await session.generateResponse('What is 2 + 2?');

        expect(response).toBeDefined();
        expect(typeof response).toBe('string');
        expect(response.length).toBeGreaterThan(0);
      });

      it('should throw error when LLM model is not loaded', async () => {
        await expect(session.generateResponse('What is 2 + 2?')).rejects.toThrow('LLM model not loaded');
      });

      it('should throw error when session is destroyed', async () => {
        session.destroy();
        await expect(session.generateResponse('What is 2 + 2?')).rejects.toThrow('Session has been destroyed');
      });
    });

    describe('synthesizeSpeech', () => {
      it('should synthesize speech when TTS model is loaded', async () => {
        await session.loadModels({
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        const audioData = await session.synthesizeSpeech('Hello world');

        expect(audioData).toBeDefined();
        expect(audioData instanceof Float32Array).toBe(true);
        expect(audioData.length).toBeGreaterThan(0);
      });

      it('should throw error when TTS model is not loaded', async () => {
        await expect(session.synthesizeSpeech('Hello world')).rejects.toThrow('TTS model not loaded');
      });

      it('should throw error when session is destroyed', async () => {
        session.destroy();
        await expect(session.synthesizeSpeech('Hello world')).rejects.toThrow('Session has been destroyed');
      });
    });

    describe('event callbacks', () => {
      it('should call registered event callbacks', async () => {
        const eventCallback = jest.fn<VoiceAgentEventCallback>();
        session.onEvent(eventCallback);

        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        await session.processVoiceTurn(mockAudioData);

        expect(eventCallback).toHaveBeenCalled();
        expect(eventCallback.mock.calls.length).toBeGreaterThan(0);

        // Check event types
        const eventTypes = eventCallback.mock.calls.map(call => call[0].type);
        expect(eventTypes).toContain('vadTriggered');
        expect(eventTypes).toContain('transcription');
        expect(eventTypes).toContain('response');
        expect(eventTypes).toContain('audioSynthesized');
      });

      it('should remove event callbacks when unregistered', async () => {
        const callback1 = jest.fn<VoiceAgentEventCallback>();
        const callback2 = jest.fn<VoiceAgentEventCallback>();

        session.onEvent(callback1);
        session.onEvent(callback2);
        session.offEvent(callback2);

        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        await session.processVoiceTurn(mockAudioData);

        expect(callback1).toHaveBeenCalled();
        expect(callback2).not.toHaveBeenCalled();
      });

      it('should handle multiple event callbacks', async () => {
        const callback1 = jest.fn<VoiceAgentEventCallback>();
        const callback2 = jest.fn<VoiceAgentEventCallback>();

        session.onEvent(callback1);
        session.onEvent(callback2);

        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        await session.processVoiceTurn(mockAudioData);

        expect(callback1).toHaveBeenCalled();
        expect(callback2).toHaveBeenCalled();
        expect(callback1).toHaveBeenCalledWith(callback2.mock.calls[0][0]);
      });
    });

    describe('destroy', () => {
      it('should cleanup all resources', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        const eventCallback = jest.fn<VoiceAgentEventCallback>();
        session.onEvent(eventCallback);

        session.destroy();

        expect(session.isReady).toBe(false);
        expect(session.getModels().stt).toBeUndefined();
        expect(session.getModels().llm).toBeUndefined();
        expect(session.getModels().tts).toBeUndefined();
        expect(session.getState()).toBe(PipelineState.Idle);
      });

      it('should prevent further operations after destroy', async () => {
        session.destroy();

        await expect(session.processVoiceTurn(mockAudioData)).rejects.toThrow('Session has been destroyed');
        await expect(session.transcribe(mockAudioData)).rejects.toThrow('Session has been destroyed');
        await expect(session.generateResponse('test')).rejects.toThrow('Session has been destroyed');
        await expect(session.synthesizeSpeech('test')).rejects.toThrow('Session has been destroyed');
      });
    });

    describe('getModels', () => {
      it('should return undefined for missing models', async () => {
        const models = session.getModels();
        expect(models.stt).toBeUndefined();
        expect(models.llm).toBeUndefined();
        expect(models.tts).toBeUndefined();
      });

      it('should return loaded models', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        const models = session.getModels();
        expect(models.stt?.id).toBe('whisper-tiny');
        expect(models.llm?.id).toBe('qwen2.5-0.5b');
        expect(models.tts?.id).toBe('piper-en');
      });
    });

    describe('getState', () => {
      it('should return initial state as Idle', () => {
        expect(session.getState()).toBe(PipelineState.Idle);
      });

      it('should return current state after operations', async () => {
        await session.loadModels({
          stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
          llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
          tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
        });

        expect(session.getState()).toBe(PipelineState.Idle);

        // Trigger state change
        const processPromise = session.processVoiceTurn(mockAudioData);

        // Check that state changed (may still be Idle if not yet started)
        const currentState = session.getState();
        expect(Object.values(PipelineState)).toContain(currentState);

        await processPromise;
      });
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('VoiceAgent factory', () => {
    it('should create a new session when SDK is initialized', async () => {
      const session = await VoiceAgent.create();
      expect(session).toBeInstanceOf(VoiceAgentSession);
      expect(session.getState()).toBe(PipelineState.Idle);
    });

    it('should throw error when SDK is not initialized', async () => {
      jest.spyOn(RunAnywhere, 'isInitialized', 'get').mockReturnValue(false);

      await expect(VoiceAgent.create()).rejects.toThrow('SDK not initialized');

      // Restore
      jest.spyOn(RunAnywhere, 'isInitialized', 'get').mockRestore();
    });

    it('should create session with pre-loaded models', async () => {
      const models: VoiceAgentModels = {
        stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
        llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
        tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' },
      };

      const session = await VoiceAgent.createWithModels(models);

      expect(session).toBeInstanceOf(VoiceAgentSession);
      expect(session.isReady).toBe(true);
      expect(session.getModels().stt?.id).toBe('whisper-tiny');
      expect(session.getModels().llm?.id).toBe('qwen2.5-0.5b');
      expect(session.getModels().tts?.id).toBe('piper-en');
    });

    it('should create session with partial models', async () => {
      const models: VoiceAgentModels = {
        stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
      };

      const session = await VoiceAgent.createWithModels(models);

      expect(session).toBeInstanceOf(VoiceAgentSession);
      expect(session.isReady).toBe(false);
      expect(session.getModels().stt).toBeDefined();
    });
  });
});
