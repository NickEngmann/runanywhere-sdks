/** Pipeline states for VoiceAgent and VoicePipeline */

export enum PipelineState {
  Idle = 'idle',
  Processing = 'processing',
  ProcessingSTT = 'processingSTT',
  VoiceDetected = 'voiceDetected',
  Transcribing = 'transcribing',
  GeneratingResponse = 'generatingResponse',
  Generating = 'generating',
  PlayingTTS = 'playingTTS',
  Synthesizing = 'synthesizing',
  TurnComplete = 'turnComplete',
  Cooldown = 'cooldown',
  Error = 'error',
  Destroyed = 'destroyed',
}
