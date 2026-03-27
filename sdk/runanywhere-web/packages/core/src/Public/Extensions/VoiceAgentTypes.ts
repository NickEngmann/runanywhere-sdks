/** RunAnywhere Web SDK - VoiceAgent Types */

import type { SpeechSegment as BaseSpeechSegment } from '../../Infrastructure/VADDetector';

export type SpeechSegment = BaseSpeechSegment;

export interface VoiceAgentModelInfo {
  path: string;
  id: string;
  name?: string;
}

export interface VoiceAgentModels {
  stt?: VoiceAgentModelInfo;
  llm?: VoiceAgentModelInfo;
  tts?: VoiceAgentModelInfo;
}

export interface VoiceTurnResult {
  speechDetected: boolean;
  transcription: string;
  response: string;
  synthesizedAudio: Float32Array;
  latencyMs: number;
}

export interface VoiceAgentEventData {
  type: 
    | 'transcription' 
    | 'response' 
    | 'audioSynthesized' 
    | 'vadTriggered'
    | 'vadSpeechStarted'
    | 'vadSpeechEnded'
    | 'vadSegmentDetected'
    | 'vadStatsUpdated'
    | 'error' 
    | 'turnComplete' 
    | 'destroyed' 
    | 'modelsLoaded';
  text?: string;
  audioData?: Float32Array;
  speechActive?: boolean;
  errorCode?: number;
  error?: unknown;
  result?: VoiceTurnResult;
  models?: VoiceAgentModels;
  segment?: {
    startIndex: number;
    endIndex: number;
    durationMs: number;
    averageEnergy: number;
  };
  vadStats?: {
    totalFrames: number;
    speechFrames: number;
    silenceFrames: number;
    detectionCount: number;
    averageEnergy: number;
    minEnergy: number;
    maxEnergy: number;
  };
}

export type VoiceAgentEventCallback = (event: VoiceAgentEventData) => void;
