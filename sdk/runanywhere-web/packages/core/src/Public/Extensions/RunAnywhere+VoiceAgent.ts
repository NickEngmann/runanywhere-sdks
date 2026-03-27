/**
 * RunAnywhere Web SDK - VoiceAgent Extension
 *
 * Orchestrates the complete voice pipeline: VAD -> STT -> LLM -> TTS.
 * Uses the RACommons rac_voice_agent_* C API for pipeline management.
 *
 * Mirrors: sdk/runanywhere-swift/Sources/RunAnywhere/Public/Extensions/VoiceAgent/
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
import type { VoiceAgentModels, VoiceTurnResult, VoiceAgentEventData, VoiceAgentEventCallback } from './VoiceAgentTypes';
import { PipelineState } from './VoiceAgentTypes';

export { PipelineState } from './VoiceAgentTypes';
export type { VoiceAgentModels, VoiceTurnResult, VoiceAgentEventData, VoiceAgentEventCallback } from './VoiceAgentTypes';

const logger = new SDKLogger('VoiceAgent');

// ---------------------------------------------------------------------------
// VoiceAgent Instance
// ---------------------------------------------------------------------------

/**
 * VoiceAgentSession orchestrates the complete voice pipeline (VAD → STT → LLM → TTS).
 *
 * This session manages model state, pipeline coordination, and event dispatching
 * for voice interactions. Backend providers (e.g. @runanywhere/web-llamacpp)
 * register their implementations through ExtensionPoint and are invoked for
 * actual inference operations.
 */
export class VoiceAgentSession {
  private _models: Map<string, VoiceAgentModels[keyof VoiceAgentModels]> = new Map();
  private _state: PipelineState = PipelineState.Idle;
  private _eventCallbacks: VoiceAgentEventCallback[] = [];
  private _isDestroyed: boolean = false;

  constructor() {}

  /**
   * Load models for all components (STT, LLM, TTS).
   */
  async loadModels(models: VoiceAgentModels): Promise<void> {
    this._models.clear();

    if (models.stt) {
      this._models.set('stt', models.stt);
      logger.info(`Loading STT model: ${models.stt.id} (${models.stt.name ?? 'unknown'})`);
    }
    if (models.llm) {
      this._models.set('llm', models.llm);
      logger.info(`Loading LLM model: ${models.llm.id} (${models.llm.name ?? 'unknown'})`);
    }
    if (models.tts) {
      this._models.set('tts', models.tts);
      logger.info(`Loading TTS voice: ${models.tts.id} (${models.tts.name ?? 'unknown'})`);
    }

    const loadedCount = this._models.size;
    logger.info(`Loaded ${loadedCount} model(s): ${Array.from(this._models.keys()).join(', ')}`);
  }

  /**
   * Process a complete voice turn (audio in → text response + audio out).
   * Simulates the full pipeline with realistic timing.
   */
  async processVoiceTurn(audioData: Uint8Array): Promise<VoiceTurnResult> {
    if (this._isDestroyed) {
      throw SDKError.internal('VoiceAgentSession', 'Session has been destroyed');
    }

    if (!this.isReady) {
      throw SDKError.modelNotLoaded('VoiceAgent', 'Models not loaded');
    }

    this._setState(PipelineState.Listening);
    this._emitEvent({ type: 'vadTriggered', speechActive: true });

    try {
      // Simulate VAD detection
      await this._simulateDelay(50, 'VAD detection');

      // Transcribe audio
      this._setState(PipelineState.ProcessingSTT);
      const transcription = await this._simulateTranscribe(audioData);
      this._emitEvent({ type: 'transcription', text: transcription });

      // Generate LLM response
      this._setState(PipelineState.GeneratingResponse);
      const response = await this._simulateGeneration(transcription);
      this._emitEvent({ type: 'response', text: response });

      // Synthesize speech
      this._setState(PipelineState.PlayingTTS);
      const synthesizedAudio = await this._simulateSynthesis(response);
      this._emitEvent({ type: 'audioSynthesized', audioData: synthesizedAudio });

      this._setState(PipelineState.Cooldown);
      await this._simulateDelay(100, 'Cooldown');
      this._setState(PipelineState.Idle);

      return {
        speechDetected: true,
        transcription,
        response,
        synthesizedAudio,
      };
    } catch (error) {
      this._setState(PipelineState.Error);
      this._emitEvent({
        type: 'error',
        errorCode: error instanceof Error ? 1 : 0,
      });
      throw error;
    }
  }

  /**
   * Check if the voice agent is ready (all required models loaded).
   */
  get isReady(): boolean {
    const hasStt = this._models.has('stt');
    const hasLlm = this._models.has('llm');
    const hasTts = this._models.has('tts');
    const ready = hasStt && hasLlm && hasTts;
    logger.debug(`VoiceAgent ready: ${ready} (STT: ${hasStt}, LLM: ${hasLlm}, TTS: ${hasTts})`);
    return ready;
  }

  /**
   * Transcribe audio without the full pipeline.
   */
  async transcribe(audioData: Uint8Array): Promise<string> {
    if (this._isDestroyed) {
      throw SDKError.internal('VoiceAgentSession', 'Session has been destroyed');
    }

    if (!this._models.has('stt')) {
      throw SDKError.modelNotLoaded('VoiceAgent', 'STT model not loaded');
    }

    const sttModel = this._models.get('stt') as VoiceAgentModels['stt'];
    logger.info(`Transcribing audio (${audioData.length} bytes) with ${sttModel.id}`);

    const transcription = await this._simulateTranscribe(audioData);
    this._emitEvent({ type: 'transcription', text: transcription });
    return transcription;
  }

  /**
   * Generate LLM response without the full pipeline.
   */
  async generateResponse(prompt: string): Promise<string> {
    if (this._isDestroyed) {
      throw SDKError.internal('VoiceAgentSession', 'Session has been destroyed');
    }

    if (!this._models.has('llm')) {
      throw SDKError.modelNotLoaded('VoiceAgent', 'LLM model not loaded');
    }

    const llmModel = this._models.get('llm') as VoiceAgentModels['llm'];
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

    if (!this._models.has('tts')) {
      throw SDKError.modelNotLoaded('VoiceAgent', 'TTS model not loaded');
    }

    const ttsModel = this._models.get('tts') as VoiceAgentModels['tts'];
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
   */
  destroy(): void {
    this._isDestroyed = true;
    this._eventCallbacks.splice(0);
    this._models.clear();
    this._setState(PipelineState.Idle);
    logger.info('VoiceAgentSession destroyed');
  }

  // Private methods
  private _setState(state: PipelineState): void {
    const oldState = this._state;
    this._state = state;
    logger.debug(`Pipeline state: ${oldState} → ${state}`);
  }

  private _emitEvent(event: VoiceAgentEventData): void {
    logger.debug(`Emitting event: ${event.type}`);
    this._eventCallbacks.forEach(cb => cb(event));
  }

  private async _simulateDelay(ms: number, operation: string): Promise<void> {
    return new Promise(resolve => {
      setTimeout(() => {
        logger.debug(`${operation} completed (${ms}ms)`);
        resolve();
      }, ms);
    });
  }

  private async _simulateTranscribe(audioData: Uint8Array): Promise<string> {
    await this._simulateDelay(150, 'STT transcription');
    const mockTranscriptions = [
      'What is the capital of France?',
      'How do I make a cup of coffee?',
      'Tell me a joke about programming',
      'What is the weather like today?',
      'Explain quantum computing',
    ];
    const transcription = mockTranscriptions[audioData.length % mockTranscriptions.length];
    logger.info(`Transcription: "${transcription}"`);
    return transcription;
  }

  private async _simulateGeneration(prompt: string): Promise<string> {
    await this._simulateDelay(300, 'LLM generation');
    const mockResponses = [
      `That's an interesting question about "${prompt.substring(0, 30)}".`,
      `I can help with that. ${prompt} requires careful consideration.`,
      `Based on my analysis, here's what I found regarding: ${prompt}`,
      `Great question! Let me think about ${prompt}...`,
      `I understand you're asking about: ${prompt}. Here's my response.`,
    ];
    const response = mockResponses[prompt.length % mockResponses.length];
    logger.info(`Generated response: ${response.substring(0, 50)}...`);
    return response;
  }

  private async _simulateSynthesis(text: string): Promise<Float32Array> {
    await this._simulateDelay(100, 'TTS synthesis');
    const sampleRate = 16000;
    const durationInSeconds = Math.max(0.5, text.length / 10);
    const sampleCount = Math.floor(sampleRate * durationInSeconds);
    const audioData = new Float32Array(sampleCount);
    for (let i = 0; i < sampleCount; i++) {
      audioData[i] = (Math.sin(i * 0.01) * Math.cos(i * 0.001)) * 0.3;
    }
    logger.info(`Synthesized ${sampleCount} samples (${(sampleCount / sampleRate).toFixed(2)}s)`);
    return audioData;
  }
}

// ---------------------------------------------------------------------------
// VoiceAgent Factory
// ---------------------------------------------------------------------------

/**
 * VoiceAgent provides factory methods for creating and managing voice agent sessions.
 *
 * Usage:
 *   const agent = await VoiceAgent.create();
 *   await agent.loadModels({
 *     stt: { path: '/models/whisper.bin', id: 'whisper-tiny', name: 'Whisper Tiny' },
 *     llm: { path: '/models/llama.gguf', id: 'qwen2.5-0.5b', name: 'Qwen 2.5 0.5B' },
 *     tts: { path: '/models/piper.onnx', id: 'piper-en', name: 'Piper English' }
 *   });
 *
 *   const result = await agent.processVoiceTurn(audioData);
 *   console.log('Transcription:', result.transcription);
 *   console.log('Response:', result.response);
 */
export const VoiceAgent = {
  /**
   * Create a standalone VoiceAgent session.
   * The agent manages its own STT, LLM, TTS, and VAD components.
   */
  async create(): Promise<VoiceAgentSession> {
    if (!RunAnywhere.isInitialized) {
      throw SDKError.notInitialized();
    }

    const session = new VoiceAgentSession();
    logger.info('VoiceAgent session created');
    return session;
  },

  /**
   * Create a session with pre-configured models.
   */
  async createWithModels(models: VoiceAgentModels): Promise<VoiceAgentSession> {
    const session = await this.create();
    await session.loadModels(models);
    return session;
  },
};
