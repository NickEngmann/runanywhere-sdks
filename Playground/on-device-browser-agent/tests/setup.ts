/**
 * Test Setup
 *
 * Configures JSDOM environment and provides polyfills for testing.
 */

// Polyfill for CSS.escape (sometimes missing in jsdom)
if (typeof globalThis.CSS === 'undefined') {
  globalThis.CSS = {} as any;
}

if (typeof globalThis.CSS.escape !== 'function') {
  globalThis.CSS.escape = function (value: string): string {
    return value.replace(/[^a-zA-Z0-9\u00A0-\uFFFF_-]/g, '\\$&');
  };
}

// Ensure jest is available in global scope for ESM
declare global {
  var jest: any;
}

// Helper to safely call jest functions
const safeJestFn = (fnName: string) => {
  if (global.jest && global.jest[fnName]) {
    return global.jest[fnName]();
  }
  return () => {};
};

// Set up a mock console for test output
const mockConsole = {
  log: safeJestFn('fn'),
  warn: safeJestFn('fn'),
  error: safeJestFn('fn'),
  debug: safeJestFn('fn'),
  info: safeJestFn('fn'),
};

// Use real console if jest is not available
if (!global.jest) {
  mockConsole.log = console.log;
  mockConsole.warn = console.warn;
  mockConsole.error = console.error;
  mockConsole.debug = console.debug;
  mockConsole.info = console.info;
}

global.console = {
  ...global.console,
  ...mockConsole,
} as any;

// Mock window.location
Object.defineProperty(globalThis.window, 'location', {
  value: {
    href: 'https://www.amazon.com/',
    origin: 'https://www.amazon.com',
    protocol: 'https:',
    host: 'www.amazon.com',
    hostname: 'www.amazon.com',
    port: '',
    pathname: '/',
    search: '',
    hash: '',
  },
  writable: true,
});

// Mock window.innerHeight
Object.defineProperty(globalThis.window, 'innerHeight', {
  value: 800,
  writable: true,
});

// Mock window.scrollBy
globalThis.window.scrollBy = global.jest?.fn() || (() => {});
globalThis.window.scrollTo = global.jest?.fn() || (() => {});

// Mock window.getComputedStyle
const originalGetComputedStyle = globalThis.window.getComputedStyle;
globalThis.window.getComputedStyle = (element: Element): any => {
  const computedStyle = originalGetComputedStyle(element);
  return {
    ...computedStyle,
    getPropertyValue: (name: string) => {
      // Default styles for test elements
      if (name === 'display') return 'block';
      if (name === 'visibility') return 'visible';
      if (name === 'opacity') return '1';
      return computedStyle.getPropertyValue(name);
    },
  };
};

// Mock ResizeObserver
class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserverMock;
