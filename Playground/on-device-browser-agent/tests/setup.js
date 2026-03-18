// Polyfill TextEncoder/TextDecoder for jsdom - MUST be first
const { TextEncoder, TextDecoder } = require('util');
global.TextEncoder = TextEncoder;
global.TextDecoder = TextDecoder || require('util').TextDecoder;

// Setup JSDOM environment
const { JSDOM } = require('jsdom');

const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>', {
  url: 'http://localhost/',
  pretendToBeVisual: true,
  resources: 'usable',
});

global.window = dom.window;
global.document = dom.window.document;
global.navigator = dom.window.navigator;

// Use native CSS.escape from window
global.CSS = dom.window.CSS || {
  escape: (str) => str,
};

// Mock window.getComputedStyle
const style = new dom.window.CSSStyleDeclaration();
Object.defineProperty(dom.window.getComputedStyle.prototype, 'getPropertyValue', {
  value: (prop) => style.getPropertyValue(prop),
});

// Mock Element.getBoundingClientRect
Object.defineProperty(dom.window.HTMLElement.prototype, 'getBoundingClientRect', {
  value: () => ({
    top: 0,
    left: 0,
    bottom: 0,
    right: 0,
    width: 100,
    height: 100,
    x: 0,
    y: 0,
    toJSON: () => ({}),
  }),
});

// Mock window.innerHeight
Object.defineProperty(global.window, 'innerHeight', {
  value: 800,
});
