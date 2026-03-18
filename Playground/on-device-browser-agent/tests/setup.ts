/// <reference types="jest" />

// Setup JSDOM environment
const { JSDOM } = require('jsdom');

const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>', {
  url: 'http://localhost/',
  pretendToBeVisual: true,
  resources: 'usable',
});

(global as any).window = dom.window;
(global as any).document = dom.window.document;
(global as any).navigator = dom.window.navigator;

// Mock CSS.escape
(global as any).CSS = {
  escape: (str: string) => str.replace(/[\x00-\x2F\x7F-\x9F]/g, (c) => '\\' + c.codePointAt(0)!.toString(16) + ' '),
};

// Mock window.getComputedStyle
const style = new dom.window.CSSStyleDeclaration();
Object.defineProperty(dom.window.getComputedStyle.prototype, 'getPropertyValue', {
  value: (prop: string) => style.getPropertyValue(prop),
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
Object.defineProperty((global as any).window, 'innerHeight', {
  value: 800,
});
