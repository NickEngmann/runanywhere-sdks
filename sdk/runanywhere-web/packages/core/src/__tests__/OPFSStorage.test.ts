/**
 * Unit tests for OPFSStorage
 */

import { OPFSStorage } from '../Infrastructure/OPFSStorage';

describe('OPFSStorage', () => {
  let storage: OPFSStorage;

  beforeEach(() => {
    storage = new OPFSStorage();
  });

  afterEach(() => {
    jest.restoreAllMocks();
    // Clear any test-specific mocks
    if (global.navigator?.storage?.getDirectory) {
      (global.navigator.storage as any).getDirectory.mockClear();
    }
  });

  describe('isSupported', () => {
    it('should return true when navigator.storage is available', () => {
      expect(OPFSStorage.isSupported).toBe(true);
    });

    it('should return false when navigator.storage is not available', () => {
      const originalStorage = global.navigator.storage;
      (global.navigator as any).storage = undefined;

      expect(OPFSStorage.isSupported).toBe(false);

      (global.navigator as any).storage = originalStorage;
    });

    it('should return false when storage exists but does not have getDirectory', () => {
      const originalStorage = global.navigator.storage;
      (global.navigator as any).storage = {};

      expect(OPFSStorage.isSupported).toBe(false);

      (global.navigator as any).storage = originalStorage;
    });
  });

  describe('isAvailable', () => {
    it('should return false before initialization', () => {
      expect(storage.isAvailable).toBe(false);
    });

    it('should return true after successful initialization', async () => {
      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue({
          getFileHandle: jest.fn().mockResolvedValue({
            createWritable: jest.fn().mockResolvedValue({
              write: jest.fn(),
              close: jest.fn(),
              abort: jest.fn(),
            }),
          }),
        }),
      });

      const result = await storage.initialize();
      expect(result).toBe(true);
      expect(storage.isAvailable).toBe(true);
    });

    it('should return false after failed initialization', async () => {
      (global.navigator as any).storage.getDirectory = jest.fn().mockRejectedValue(new Error('Permission denied'));

      const result = await storage.initialize();
      expect(result).toBe(false);
      expect(storage.isAvailable).toBe(false);
    });
  });

  describe('initialize', () => {
    it('should initialize OPFS root and models directory', async () => {
      const mockDirectoryHandle = {
        getDirectoryHandle: jest.fn().mockResolvedValue({
          getFileHandle: jest.fn().mockResolvedValue({
            createWritable: jest.fn().mockResolvedValue({
              write: jest.fn(),
              close: jest.fn(),
              abort: jest.fn(),
            }),
          }),
        }),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue(mockDirectoryHandle);

      const result = await storage.initialize();
      expect(result).toBe(true);
      expect(mockDirectoryHandle.getDirectoryHandle).toHaveBeenCalledWith('models', { create: true });
      expect(storage.isAvailable).toBe(true);
    });

    it('should cache the availability status', async () => {
      (global.navigator as any).storage.getDirectory = jest.fn().mockRejectedValue(new Error('Failed'));

      await storage.initialize();
      expect(storage.isAvailable).toBe(false);

      // Second call should return cached value without trying to initialize
      const result = await storage.initialize();
      expect(result).toBe(false);
    });

    it('should handle initialization errors gracefully', async () => {
      (global.navigator as any).storage.getDirectory = jest.fn().mockRejectedValue(new Error('QuotaExceededError'));

      const result = await storage.initialize();
      expect(result).toBe(false);
      expect(storage.isAvailable).toBe(false);
    });
  });

  describe('saveModel', () => {
    it('should throw error if OPFS not initialized', async () => {
      await expect(storage.saveModel('test', new ArrayBuffer(1000))).rejects.toThrow(
        'OPFS not initialized. Call initialize() first.'
      );
    });

    it('should save model data to OPFS', async () => {
      const mockFileHandle = {
        createWritable: jest.fn().mockResolvedValue({
          write: jest.fn(),
          close: jest.fn(),
        }),
      };

      const mockDirectoryHandle = {
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockDirectoryHandle),
      });

      await storage.initialize();

      const testData = new ArrayBuffer(1000);
      await storage.saveModel('test-model', testData);

      expect(mockFileHandle.createWritable).toHaveBeenCalled();
      expect(mockDirectoryHandle.getFileHandle).toHaveBeenCalledWith('test-model', { create: true });
    });

    it('should handle save errors and cleanup corrupted files', async () => {
      const mockFileHandle = {
        createWritable: jest.fn().mockResolvedValue({
          write: jest.fn().mockRejectedValue(new Error('Write failed')),
          close: jest.fn(),
          abort: jest.fn(),
        }),
      };

      const mockDirectoryHandle = {
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockDirectoryHandle),
      });

      await storage.initialize();

      const testData = new ArrayBuffer(1000);
      await expect(storage.saveModel('test-model', testData)).rejects.toThrow('Write failed');

      expect(mockDirectoryHandle.removeEntry).toHaveBeenCalled();
    });

    it('should support nested paths', async () => {
      const mockFileHandle = {
        createWritable: jest.fn().mockResolvedValue({
          write: jest.fn(),
          close: jest.fn(),
        }),
      };

      const mockLevel3 = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockFileHandle),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      const mockLevel2 = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockLevel3),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      const mockLevel1 = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockLevel2),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      const mockModelsDir = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockLevel1),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockModelsDir),
      });

      await storage.initialize();

      await storage.saveModel('org/model/subdir/file.gguf', new ArrayBuffer(1000));

      expect(mockModelsDir.getDirectoryHandle).toHaveBeenCalledWith('org', { create: true });
      expect(mockLevel1.getDirectoryHandle).toHaveBeenCalledWith('model', { create: true });
      expect(mockLevel2.getDirectoryHandle).toHaveBeenCalledWith('subdir', { create: true });
      expect(mockFileHandle.createWritable).toHaveBeenCalled();
    });
  });

  describe('loadModel', () => {
    it('should load model data from OPFS', async () => {
      const mockFileHandle = {
        getFile: jest.fn().mockResolvedValue(new File(['test data'], 'test-model')),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue({
          getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        }),
      });

      await storage.initialize();

      const result = await storage.loadModel('test-model');
      expect(result).toBeDefined();
      expect(result?.byteLength).toBeGreaterThan(0);
    });

    it('should return null if model does not exist', async () => {
      const mockFileHandle = {
        getFile: jest.fn().mockRejectedValue(new Error('File not found')),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue({
          getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        }),
      });

      await storage.initialize();

      const result = await storage.loadModel('non-existent');
      expect(result).toBeNull();
    });
  });

  describe('listModels', () => {
    it('should list all models in OPFS', async () => {
      const mockFile1 = {
        kind: 'file' as const,
        name: 'model1',
        getFile: jest.fn().mockResolvedValue(new File(['data1'], 'model1')),
      };

      const mockFile2 = {
        kind: 'file' as const,
        name: 'model2',
        getFile: jest.fn().mockResolvedValue(new File(['data2'], 'model2')),
      };

      // entries() should return an async iterator
      const mockAsyncIterator = {
        next: jest.fn()
          .mockResolvedValueOnce({ value: ['model1', mockFile1], done: false })
          .mockResolvedValueOnce({ value: ['model2', mockFile2], done: false })
          .mockResolvedValueOnce({ done: true }),
      };

      const mockEntries = {
        [Symbol.asyncIterator]: jest.fn().mockReturnValue(mockAsyncIterator),
      };

      const mockParentDir = {
        entries: jest.fn().mockReturnValue(mockEntries),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockParentDir),
      });

      await storage.initialize();

      const models = await storage.listModels();
      expect(models).toHaveLength(2);
      expect(models.map(m => m.id)).toContain('model1');
      expect(models.map(m => m.id)).toContain('model2');
    });
  });

  describe('deleteModel', () => {
    it('should delete model from OPFS', async () => {
      const mockParentDir = {
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockParentDir),
      });

      await storage.initialize();

      await storage.deleteModel('test-model');

      expect(mockParentDir.removeEntry).toHaveBeenCalledWith('test-model');
    });
  });

  describe('hasModel', () => {
    it('should return true if model exists', async () => {
      const mockParentDir = {
        getFileHandle: jest.fn().mockResolvedValue({
          getFile: jest.fn().mockResolvedValue(new File(['data'], 'model')),
        }),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockParentDir),
      });

      await storage.initialize();

      const exists = await storage.hasModel('test-model');
      expect(exists).toBe(true);
    });

    it('should return false if model does not exist', async () => {
      const mockParentDir = {
        getFileHandle: jest.fn().mockRejectedValue(new Error('File not found')),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockParentDir),
      });

      await storage.initialize();

      const exists = await storage.hasModel('non-existent');
      expect(exists).toBe(false);
    });
  });

  describe('getStorageUsage', () => {
    it('should return storage usage estimates', async () => {
      const mockParentDir = {
        getFileHandle: jest.fn().mockResolvedValue({
          getFile: jest.fn().mockResolvedValue(new File(['data'], 'model')),
        }),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockParentDir),
      });

      (navigator.storage as any).estimate = jest.fn().mockResolvedValue({
        quota: 10737418240, // 10GB
        usage: 1048576, // 1MB
      });

      await storage.initialize();

      const result = await storage.getStorageUsage();
      expect(result).toBeDefined();
      expect(result.quotaBytes).toBe(10737418240);
      expect(result.usedBytes).toBeGreaterThan(0);
    });
  });

  describe('resolveParentDir and resolveFilename helpers', () => {
    it('should handle flat keys correctly', async () => {
      const mockParentDir = {
        getFileHandle: jest.fn().mockResolvedValue({
          createWritable: jest.fn().mockResolvedValue({
            write: jest.fn(),
            close: jest.fn(),
          }),
        }),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockParentDir),
      });

      await storage.initialize();

      await storage.saveModel('simple-model', new ArrayBuffer(1000));
      expect(mockParentDir.getFileHandle).toHaveBeenCalledWith('simple-model', { create: true });
    });

    it('should create nested directories for nested keys', async () => {
      const mockFileHandle = {
        createWritable: jest.fn().mockResolvedValue({
          write: jest.fn(),
          close: jest.fn(),
        }),
        getFileHandle: jest.fn().mockResolvedValue({
          createWritable: jest.fn().mockResolvedValue({
            write: jest.fn(),
            close: jest.fn(),
          }),
        }),
      };

      const mockLevel4 = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockFileHandle),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      const mockLevel3 = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockLevel4),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      const mockLevel2 = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockLevel3),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      const mockLevel1 = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockLevel2),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      const mockModelsDir = {
        getDirectoryHandle: jest.fn().mockResolvedValue(mockLevel1),
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockModelsDir),
      });

      await storage.initialize();

      await storage.saveModel('a/b/c/d/file.gguf', new ArrayBuffer(1000));

      // Verify the call chain for creating nested directories
      expect(mockModelsDir.getDirectoryHandle).toHaveBeenCalledWith('a', { create: true });
      expect(mockLevel1.getDirectoryHandle).toHaveBeenCalledWith('b', { create: true });
      expect(mockLevel2.getDirectoryHandle).toHaveBeenCalledWith('c', { create: true });
      expect(mockLevel3.getDirectoryHandle).toHaveBeenCalledWith('d', { create: true });

      // Verify final file creation
      expect(mockFileHandle.createWritable).toHaveBeenCalled();
    });
  });

  describe('Edge cases', () => {
    it('should handle large model files', async () => {
      const mockFileHandle = {
        createWritable: jest.fn().mockResolvedValue({
          write: jest.fn(),
          close: jest.fn(),
        }),
      };

      const mockParentDir = {
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockParentDir),
      });

      await storage.initialize();

      // 1GB model
      const largeData = new ArrayBuffer(1024 * 1024 * 1024);
      await storage.saveModel('large-model', largeData);

      expect(mockFileHandle.createWritable).toHaveBeenCalled();
    });

    it('should handle empty model files', async () => {
      const mockFileHandle = {
        createWritable: jest.fn().mockResolvedValue({
          write: jest.fn(),
          close: jest.fn(),
        }),
      };

      const mockParentDir = {
        getFileHandle: jest.fn().mockResolvedValue(mockFileHandle),
        removeEntry: jest.fn(),
      };

      (global.navigator as any).storage.getDirectory = jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue(mockParentDir),
      });

      await storage.initialize();

      const emptyData = new ArrayBuffer(0);
      await storage.saveModel('empty-model', emptyData);

      expect(mockFileHandle.createWritable).toHaveBeenCalled();
    });
  });
});
