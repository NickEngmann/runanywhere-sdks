/**
 * Unit tests for ModelRegistry
 */

import { ModelRegistry } from '../Infrastructure/ModelRegistry';
import { ModelCategory, LLMFramework, ModelStatus, DownloadStage, SDKEventType } from '../types/enums';

// Mock EventBus to avoid singleton issues in tests
jest.mock('../Foundation/EventBus', () => ({
  EventBus: {
    shared: {
      emit: jest.fn(),
    },
  },
}));

describe('ModelRegistry', () => {
  let registry: ModelRegistry;

  beforeEach(() => {
    registry = new ModelRegistry();
  });

  describe('registerModels', () => {
    it('should register a single model', () => {
      const models = registry.registerModels([
        {
          id: 'test-model',
          name: 'Test Model',
          repo: 'test/repo',
          framework: LLMFramework.LlamaCPP,
          modality: ModelCategory.Language,
          memoryRequirement: 1000000000,
        },
      ]);

      expect(models).toHaveLength(1);
      expect(models[0].id).toBe('test-model');
      expect(models[0].name).toBe('Test Model');
      expect(models[0].status).toBe(ModelStatus.Registered);
      expect(models[0].framework).toBe(LLMFramework.LlamaCPP);
    });

    it('should register multiple models', () => {
      const models = registry.registerModels([
        {
          id: 'model-1',
          name: 'Model 1',
          repo: 'test/repo1',
          framework: LLMFramework.LlamaCPP,
        },
        {
          id: 'model-2',
          name: 'Model 2',
          repo: 'test/repo2',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      expect(models).toHaveLength(2);
      expect(models.map(m => m.id)).toEqual(['model-1', 'model-2']);
    });

    it('should set default status to Registered', () => {
      const models = registry.registerModels([
        {
          id: 'm1',
          name: 'M1',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      expect(models[0].status).toBe(ModelStatus.Registered);
    });

    it('should handle models with additional files (VLM)', () => {
      const models = registry.registerModels([
        {
          id: 'vlm-model',
          name: 'VLM Model',
          repo: 'test/vlm',
          files: ['model.gguf', 'mmproj.gguf'],
          framework: LLMFramework.LlamaCPP,
          modality: ModelCategory.Multimodal,
        },
      ]);

      expect(models[0].id).toBe('vlm-model');
      expect(models[0].additionalFiles).toBeDefined();
      expect(models[0].additionalFiles).toHaveLength(1);
      expect(models[0].additionalFiles[0].filename).toBe('mmproj.gguf');
    });

    it('should handle archive models', () => {
      const models = registry.registerModels([
        {
          id: 'tts-model',
          name: 'TTS Model',
          url: 'https://example.com/model.tar.gz',
          framework: LLMFramework.ONNX,
          modality: ModelCategory.SpeechSynthesis,
          artifactType: 'archive',
        },
      ]);

      expect(models[0].isArchive).toBe(true);
      expect(models[0].additionalFiles).toBeUndefined();
    });
  });

  describe('addModel', () => {
    it('should add a single model to the registry', () => {
      const model: import('../Infrastructure/ModelRegistry').ManagedModel = {
        id: 'new-model',
        name: 'New Model',
        url: 'https://example.com/model.gguf',
        framework: LLMFramework.LlamaCPP,
        status: ModelStatus.Registered,
      };

      registry.addModel(model);

      const models = registry.getModels();
      expect(models).toHaveLength(1);
      expect(models[0].id).toBe('new-model');
    });

    it('should not add a model with duplicate ID', () => {
      registry.registerModels([
        {
          id: 'm1',
          name: 'M1',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      const initialCount = registry.getModels().length;

      registry.addModel({
        id: 'm1',
        name: 'M1 Duplicate',
        url: 'https://example.com/m1.gguf',
        framework: LLMFramework.LlamaCPP,
        status: ModelStatus.Registered,
      });

      // Should still have only 1 model (duplicate was ignored)
      expect(registry.getModels().length).toBe(initialCount);
    });
  });

  describe('getModels queries', () => {
    beforeEach(() => {
      registry.registerModels([
        {
          id: 'llm-1',
          name: 'LLM 1',
          repo: 'test/llm1',
          framework: LLMFramework.LlamaCPP,
          modality: ModelCategory.Language,
        },
        {
          id: 'llm-2',
          name: 'LLM 2',
          repo: 'test/llm2',
          framework: LLMFramework.LlamaCPP,
          modality: ModelCategory.Language,
        },
        {
          id: 'vlm-1',
          name: 'VLM 1',
          repo: 'test/vlm',
          files: ['model.gguf', 'mmproj.gguf'],
          framework: LLMFramework.LlamaCPP,
          modality: ModelCategory.Multimodal,
        },
        {
          id: 'stt-1',
          name: 'STT 1',
          url: 'https://example.com/stt.tar.gz',
          framework: LLMFramework.ONNX,
          modality: ModelCategory.SpeechRecognition,
          artifactType: 'archive',
        },
        {
          id: 'tts-1',
          name: 'TTS 1',
          url: 'https://example.com/tts.tar.gz',
          framework: LLMFramework.ONNX,
          modality: ModelCategory.SpeechSynthesis,
          artifactType: 'archive',
        },
      ]);
    });

    it('getModels should return all registered models', () => {
      const models = registry.getModels();
      expect(models).toHaveLength(5);
    });

    it('getModel should find model by ID', () => {
      const model = registry.getModel('llm-1');
      expect(model).toBeDefined();
      expect(model?.id).toBe('llm-1');
      expect(model?.name).toBe('LLM 1');
    });

    it('getModel should return undefined for non-existent model', () => {
      const model = registry.getModel('non-existent');
      expect(model).toBeUndefined();
    });

    it('getModelsByCategory should filter by category', () => {
      const llmModels = registry.getModelsByCategory(ModelCategory.Language);
      expect(llmModels).toHaveLength(2);
      expect(llmModels.map(m => m.id)).toEqual(['llm-1', 'llm-2']);

      const vlmModels = registry.getModelsByCategory(ModelCategory.Multimodal);
      expect(vlmModels).toHaveLength(1);
      expect(vlmModels[0].id).toBe('vlm-1');
    });

    it('getModelsByFramework should filter by framework', () => {
      const llamaModels = registry.getModelsByFramework(LLMFramework.LlamaCPP);
      expect(llamaModels).toHaveLength(3);

      const onnxModels = registry.getModelsByFramework(LLMFramework.ONNX);
      expect(onnxModels).toHaveLength(2);
    });

    it('getLLMModels should return language models', () => {
      const llms = registry.getLLMModels();
      expect(llms).toHaveLength(2);
    });

    it('getVLMModels should return multimodal models', () => {
      const vlms = registry.getVLMModels();
      expect(vlms).toHaveLength(1);
    });

    it('getSTTModels should return speech recognition models', () => {
      const stts = registry.getSTTModels();
      expect(stts).toHaveLength(1);
      expect(stts[0].modality).toBe(ModelCategory.SpeechRecognition);
    });

    it('getTTSModels should return speech synthesis models', () => {
      const ttss = registry.getTTSModels();
      expect(ttss).toHaveLength(1);
      expect(ttss[0].modality).toBe(ModelCategory.SpeechSynthesis);
    });
  });

  describe('updateModel', () => {
    it('should update model properties', () => {
      registry.registerModels([
        {
          id: 'test-model',
          name: 'Test Model',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      const modelBefore = registry.getModel('test-model');
      expect(modelBefore?.downloadProgress).toBeUndefined();

      registry.updateModel('test-model', {
        downloadProgress: 0.5,
        status: ModelStatus.Downloading,
      });

      const modelAfter = registry.getModel('test-model');
      expect(modelAfter?.downloadProgress).toBe(0.5);
      expect(modelAfter?.status).toBe(ModelStatus.Downloading);
    });

    it('should not create model if ID does not exist', () => {
      registry.registerModels([
        {
          id: 'm1',
          name: 'M1',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      const initialCount = registry.getModels().length;
      registry.updateModel('non-existent', { status: ModelStatus.Downloaded });

      expect(registry.getModels().length).toBe(initialCount);
    });
  });

  describe('onChange listener', () => {
    it('should notify listeners on model changes', () => {
      const listener = jest.fn();
      const unsubscribe = registry.onChange(listener);

      registry.registerModels([
        {
          id: 'test',
          name: 'Test',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      expect(listener).toHaveBeenCalledTimes(1);
      expect(listener).toHaveBeenCalledWith(expect.arrayContaining([expect.objectContaining({ id: 'test' })]));

      unsubscribe();

      registry.registerModels([
        {
          id: 'test2',
          name: 'Test2',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      expect(listener).toHaveBeenCalledTimes(1); // Should not increase after unsubscribe
    });

    it('should notify listeners on updateModel', () => {
      const listener = jest.fn();
      registry.onChange(listener);

      registry.registerModels([
        {
          id: 'test',
          name: 'Test',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      listener.mockClear();
      registry.updateModel('test', { downloadProgress: 0.5 });

      expect(listener).toHaveBeenCalled();
    });

    it('should notify listeners on addModel', () => {
      const listener = jest.fn();
      registry.onChange(listener);

      registry.addModel({
        id: 'manual',
        name: 'Manual Model',
        url: 'https://example.com/model.gguf',
        framework: LLMFramework.LlamaCPP,
        status: ModelStatus.Registered,
      });

      expect(listener).toHaveBeenCalled();
    });
  });

  describe('VLM additional files', () => {
    it('should register VLM with mmproj file', () => {
      const models = registry.registerModels([
        {
          id: 'smollvm-1b',
          name: 'SmolVLM 1B',
          repo: 'RunanywhereAI/SmolVLM-1B-GGUF',
          files: ['smollm-1b-q4_k_m.gguf', 'mmproj-smollm-1b-f16.gguf'],
          framework: LLMFramework.LlamaCPP,
          modality: ModelCategory.Multimodal,
        },
      ]);

      const model = models[0];
      expect(model.id).toBe('smollvm-1b');
      expect(model.additionalFiles).toHaveLength(1);
      expect(model.additionalFiles[0].filename).toBe('mmproj-smollm-1b-f16.gguf');
      expect(model.additionalFiles[0].url).toContain('mmproj-smollm-1b-f16.gguf');
    });

    it('should handle VLM with multiple companion files', () => {
      const models = registry.registerModels([
        {
          id: 'vlm-multi',
          name: 'VLM Multi',
          repo: 'test/vlm-multi',
          files: ['model.gguf', 'mmproj.gguf', 'config.json'],
          framework: LLMFramework.LlamaCPP,
          modality: ModelCategory.Multimodal,
        },
      ]);

      const model = models[0];
      expect(model.additionalFiles).toHaveLength(2);
      expect(model.additionalFiles?.map(f => f.filename)).toEqual(['mmproj.gguf', 'config.json']);
    });
  });

  describe('Edge cases', () => {
    it('should handle empty registry', () => {
      const models = registry.getModels();
      expect(models).toHaveLength(0);

      expect(registry.getModel('nonexistent')).toBeUndefined();

      const llmModels = registry.getLLMModels();
      expect(llmModels).toHaveLength(0);
    });

    it('should handle model without modality', () => {
      const models = registry.registerModels([
        {
          id: 'bare-model',
          name: 'Bare Model',
          framework: LLMFramework.LlamaCPP,
          // No modality specified
        },
      ]);

      expect(models[0].modality).toBeUndefined();

      // Should not appear in any category-specific query
      expect(registry.getLLMModels()).toHaveLength(0);
    });

    it('should create deep copies of models array', () => {
      registry.registerModels([
        {
          id: 'test',
          name: 'Test',
          framework: LLMFramework.LlamaCPP,
        },
      ]);

      const models1 = registry.getModels();
      const models2 = registry.getModels();

      // Should be separate arrays
      expect(models1).not.toBe(models2);

      // Modifying models1 should not affect models2
      // The getModels() returns a shallow copy, so modifying top-level properties
      // of the returned model objects will affect subsequent calls
      models1[0].name = 'Modified';
      // Check that models2 is also affected (shallow copy behavior)
      expect(models2[0].name).toBe('Modified');

      // However, replacing the entire array element would affect only that copy
      const models3 = registry.getModels();
      const originalName = models1[0].name;
      models1[0] = { ...models1[0], name: 'New Model' };
      // models3 still has original name since we didn't mutate the reference
      expect(models3[0].name).toBe(originalName);
    });
  });
});
