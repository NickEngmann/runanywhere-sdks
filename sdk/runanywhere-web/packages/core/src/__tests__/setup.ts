/**
 * Jest setup file for @runanywhere/web
 */

// Mock browser APIs that may not be available in test environment

// Mock fetch
(global as any).fetch = jest.fn();

// Mock navigator with storage API - make it writable for tests
Object.defineProperty(global, 'navigator', {
  value: {
    storage: {
      getDirectory: jest.fn().mockResolvedValue({
        getDirectoryHandle: jest.fn().mockResolvedValue({
          getFileHandle: jest.fn().mockResolvedValue({
            createWritable: jest.fn().mockResolvedValue({
              write: jest.fn(),
              close: jest.fn(),
              abort: jest.fn(),
            }),
          }),
          removeEntry: jest.fn(),
        }),
      }),
      estimate: jest.fn().mockResolvedValue({
        quota: 10737418240, // 10GB
        usage: 0,
      }),
    },
  },
  writable: true,
  configurable: true,
});

// Mock URL
(global as any).URL = require('url').URL;

// Mock ReadableStream for testing streaming functionality
(global as any).ReadableStream = class ReadableStream {
  constructor() {}
  getReader() {
    return {
      read: jest.fn().mockResolvedValue({ done: true, value: undefined }),
      releaseLock: jest.fn(),
    };
  }
};

// Mock File API
(global as any).File = class File {
  constructor(parts: any[], filename: string) {
    (this as any).name = filename;
    (this as any).size = parts[0]?.length || 0;
    (this as any).type = 'application/octet-stream';
    (this as any).lastModified = Date.now();
  }

  arrayBuffer() {
    return Promise.resolve((this as any).size > 0 ? new ArrayBuffer((this as any).size) : new ArrayBuffer(0));
  }

  stream() {
    return new ReadableStream();
  }
};

// Mock IndexedDB for OPFS
(global as any).indexedDB = {
  open: jest.fn(),
};

// Mock File System Access API
(global as any).window = {
  showOpenFilePicker: jest.fn(),
  showDirectoryPicker: jest.fn(),
};
