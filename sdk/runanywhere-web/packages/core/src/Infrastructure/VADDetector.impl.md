/**
 * VAD (Voice Activity Detection) Implementation
 * 
 * Added real voice activity detection to the VoiceAgent pipeline:
 * 
 * 1. VADDetector (src/Infrastructure/VADDetector.ts):
 *    - Energy-based voice detection with configurable thresholds
 *    - Auto-calibration for adapting to environment noise
 *    - State machine: Silence -> PreSpeech -> Speech -> PostSpeech -> Silence
 *    - Speech segment detection with statistics
 * 
 * 2. VoiceAgentSession integration:
 *    - Real VAD processing in processVoiceTurn()
 *    - Silent audio detection (skips transcription if no voice)
 *    - VAD event callbacks (vadTriggered, vadSpeechStarted, vadSpeechEnded, vadSegmentDetected)
 *    - getVADDetector() for advanced access
 * 
 * 3. Updated types (src/Public/Extensions/VoiceAgentTypes.ts):
 *    - New event types for VAD: vadSpeechStarted, vadSpeechEnded, vadSegmentDetected, vadStatsUpdated
 *    - Event data for segments and statistics
 * 
 * 4. Tests:
 *    - Comprehensive VADDetector tests (21 tests)
 *    - Updated VoiceAgent tests to mock VAD for existing tests (33 tests)
 *    - All 54 tests pass
 * 
 * Usage Example:
 *   const agent = await VoiceAgent.create();
 *   await agent.loadModels({ stt, llm, tts });
 *   
 *   // VAD will automatically detect voice in audio
 *   const result = await agent.processVoiceTurn(audioData);
 *   
 *   // Access VAD detector for advanced usage
 *   const vad = agent.getVADDetector();
 *   const segments = vad?.processAudio(audioData);
 * 
 * Benefits:
 * - Reduces unnecessary processing for silent audio
 * - Provides fine-grained voice activity awareness
 * - Adapts to environmental noise levels
 * - Tracks detection statistics for quality monitoring
 */
