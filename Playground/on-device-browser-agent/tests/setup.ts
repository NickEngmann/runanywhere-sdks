/**
 * Test setup file
 * Provides mocks for browser APIs used in DOM observer tests
 */

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Mock IntersectionObserver
class MockIntersectionObserver {
  root = null;
  rootMargin = '0px';
  thresholds = [0];

  observe = jest.fn((target) => {
    // Simulate intersection for all elements
    this.callbackQueue = this.callbackQueue || [];
    this.callbackQueue.push([
      {
        isIntersecting: true,
        target,
        boundingClientRect: {
          width: 100,
          height: 50,
          top: 0,
          right: 100,
          bottom: 50,
          left: 0,
          x: 0,
          y: 0,
        },
        intersectionRect: {
          width: 100,
          height: 50,
          top: 0,
          right: 100,
          bottom: 50,
          left: 0,
          x: 0,
          y: 0,
        },
        rootBounds: null,
      },
    ]);
  });

  unobserve = jest.fn();
  disconnect = jest.fn();
  takeRecords = jest.fn();
}

Object.defineProperty(window, 'IntersectionObserver', {
  writable: true,
  value: MockIntersectionObserver,
});

// Mock getComputedStyle
const originalGetComputedStyle = window.getComputedStyle;
Object.defineProperty(window, 'getComputedStyle', {
  writable: true,
  value: (element) => ({
    display: element.style?.display || 'block',
    visibility: element.style?.visibility || 'visible',
    opacity: element.style?.opacity !== '0' ? '1' : '0',
    width: element.getBoundingClientRect?.().width || '100px',
    height: element.getBoundingClientRect?.().height || '50px',
    ...(element.style || {}),
  }),
});

// Mock CSS.escape
global.CSS = {
  escape: jest.fn((str) => {
    return str.replace(/[!"#$%&'()*+,.\/:;<=>?@[\\\]^`{|}~]/g, '\\$&');
  }),
};
