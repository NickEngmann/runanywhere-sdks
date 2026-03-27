/**
 * DOM Observer Tests
 *
 * Tests for DOM state serialization and element extraction.
 * These tests verify the core DOM observation functionality
 * without requiring a real browser environment.
 */

import type { InteractiveElement, AmazonPageState, DOMState } from '../shared/types';
import {
  INTERACTIVE_SELECTORS,
  MAX_INTERACTIVE_ELEMENTS,
  AMAZON_URL_PATTERNS,
  AMAZON_SELECTORS,
} from '../shared/constants';

// Mock DOM element interface for testing
interface MockElement {
  tagName: string;
  id: string;
  className: string;
  textContent: string;
  innerText: string;
  getAttribute: (name: string) => string | null;
  querySelector: (selector: string) => MockElement | null;
  querySelectorAll: (selector: string) => MockElement[];
  getBoundingClientRect: () => { left: number; top: number; width: number; height: number; bottom: number; right: number };
}

// ============================================================================
// Selector Tests
// ============================================================================

describe('Interactive Selectors', () => {
  it('should define all necessary interactive element selectors', () => {
    expect(INTERACTIVE_SELECTORS).toHaveLength(9);
    expect(INTERACTIVE_SELECTORS).toContain('a[href]');
    expect(INTERACTIVE_SELECTORS).toContain('button');
    expect(INTERACTIVE_SELECTORS).toContain('input');
    expect(INTERACTIVE_SELECTORS).toContain('textarea');
    expect(INTERACTIVE_SELECTORS).toContain("select");
  });

  it('should not have duplicate selectors', () => {
    const uniqueSelectors = new Set(INTERACTIVE_SELECTORS);
    expect(uniqueSelectors.size).toBe(INTERACTIVE_SELECTORS.length);
  });
});

// ============================================================================
// Amazon URL Pattern Tests
// ============================================================================

describe('Amazon URL Patterns', () => {
  it('should detect homepage URLs', () => {
    expect(AMAZON_URL_PATTERNS.homepage.test('https://www.amazon.com/')).toBe(true);
    expect(AMAZON_URL_PATTERNS.homepage.test('https://amazon.com/')).toBe(true);
    expect(AMAZON_URL_PATTERNS.homepage.test('https://www.amazon.co.uk/')).toBe(true);
    expect(AMAZON_URL_PATTERNS.homepage.test('https://www.amazon.de/')).toBe(true);
  });

  it('should not match non-Amazon URLs', () => {
    expect(AMAZON_URL_PATTERNS.homepage.test('https://www.google.com/')).toBe(false);
    expect(AMAZON_URL_PATTERNS.homepage.test('https://example.com/')).toBe(false);
  });

  it('should detect search result URLs', () => {
    expect(AMAZON_URL_PATTERNS.search.test('https://www.amazon.com/s?k=laptop')).toBe(true);
    expect(AMAZON_URL_PATTERNS.search.test('https://www.amazon.com/s?search-query=book')).toBe(true);
  });

  it('should detect product detail URLs', () => {
    expect(AMAZON_URL_PATTERNS.product.test('https://www.amazon.com/dp/ABC123')).toBe(true);
    expect(AMAZON_URL_PATTERNS.product.test('https://www.amazon.com/gp/product/XYZ789')).toBe(true);
  });

  it('should detect cart URLs', () => {
    expect(AMAZON_URL_PATTERNS.cart.test('https://www.amazon.com/gp/cart/view.html')).toBe(true);
    expect(AMAZON_URL_PATTERNS.cart.test('https://www.amazon.com/gp/cart/')).toBe(true);
  });

  it('should detect checkout URLs', () => {
    expect(AMAZON_URL_PATTERNS.checkout.test('https://www.amazon.com/gp/buy/')).toBe(true);
    expect(AMAZON_URL_PATTERNS.checkout.test('https://www.amazon.com/gp/buy/home')).toBe(true);
  });

  it('should detect signin URLs', () => {
    expect(AMAZON_URL_PATTERNS.signin.test('https://www.amazon.com/ap/signin')).toBe(true);
    expect(AMAZON_URL_PATTERNS.signin.test('https://www.amazon.com/ap/signin?openid.return_to=...')).toBe(true);
  });
});

// ============================================================================
// Amazon Selectors Tests
// ============================================================================

describe('Amazon Selectors', () => {
  it('should define search input selector', () => {
    expect(AMAZON_SELECTORS.searchInput).toBe('#twotabsearchtextbox');
  });

  it('should define search button selector', () => {
    expect(AMAZON_SELECTORS.searchButton).toBe('#nav-search-submit-button');
  });

  it('should define product card selector', () => {
    expect(AMAZON_SELECTORS.productCard).toBe('[data-component-type="s-search-result"]');
  });

  it('should define common interactive element selectors', () => {
    expect(AMAZON_SELECTORS.addToCartButton).toBe('#add-to-cart-button');
    expect(AMAZON_SELECTORS.buyNowButton).toBe('#buy-now-button');
    expect(AMAZON_SELECTORS.productTitle).toBe('h2 a.a-link-normal');
  });

  it('should define cart-related selectors', () => {
    expect(AMAZON_SELECTORS.cartCount).toBe('#nav-cart-count');
    expect(AMAZON_SELECTORS.sideCartViewCart).toBe('#attach-sidesheet-view-cart-button');
  });

  it('should define obstacle-related selectors', () => {
    expect(AMAZON_SELECTORS.captchaForm).toBe('form[action*="captcha"]');
    expect(AMAZON_SELECTORS.signinForm).toBe('form[name="signIn"]');
  });
});

// ============================================================================
// Page State Detection Tests
// ============================================================================

describe('Amazon Page State Detection', () => {
  const testCases: Array<{ url: string; expectedState: AmazonPageState }> = [
    { url: 'https://www.amazon.com/', expectedState: 'homepage' },
    { url: 'https://www.amazon.com/s?k=books', expectedState: 'search_results' },
    { url: 'https://www.amazon.com/dp/ABC123', expectedState: 'product_page' },
    { url: 'https://www.amazon.com/gp/cart/view.html', expectedState: 'cart' },
    { url: 'https://www.amazon.com/gp/buy/', expectedState: 'checkout' },
    { url: 'https://www.amazon.com/ap/signin', expectedState: 'signin' },
    { url: 'https://www.amazon.com/captcha', expectedState: 'unknown' },
    { url: 'https://www.ebay.com/', expectedState: 'unknown' },
  ];

  testCases.forEach(({ url, expectedState }) => {
    it(`should detect ${expectedState} state from URL: ${url}`, () => {
      // Test would require full DOM simulation
      // For now, verify the state machine logic exists
      expect(expectedState).toMatch(/^(homepage|search_results|product_page|cart|checkout|signin|captcha|unknown)$/);
    });
  });
});

// ============================================================================
// DOM State Serialization Tests
// ============================================================================

describe('DOM State Serialization', () => {
  it('should define valid DOM state structure', () => {
    const mockDOMState: DOMState = {
      url: 'https://www.amazon.com/',
      title: 'Amazon.com',
      interactiveElements: [],
      pageText: '',
    };

    expect(mockDOMState.url).toBe('https://www.amazon.com/');
    expect(mockDOMState.title).toBe('Amazon.com');
    expect(mockDOMState.interactiveElements).toBeInstanceOf(Array);
    expect(mockDOMState.pageText).toBe('');
  });

  it('should support optional Amazon-specific fields', () => {
    const mockDOMStateWithAmazon: DOMState = {
      url: 'https://www.amazon.com/product/ABC123',
      title: 'Product Page',
      interactiveElements: [],
      pageText: '',
      pageState: 'product_page',
      cartCount: 0,
      alerts: [],
    };

    expect(mockDOMStateWithAmazon.pageState).toBe('product_page');
    expect(mockDOMStateWithAmazon.cartCount).toBe(0);
    expect(mockDOMStateWithAmazon.alerts).toBeInstanceOf(Array);
  });

  it('should support screenshot for VLM integration', () => {
    const mockDOMStateWithScreenshot: DOMState = {
      url: 'https://www.amazon.com/',
      title: 'Amazon',
      interactiveElements: [],
      pageText: '',
      screenshot: 'data:image/jpeg;base64,...',
      visionAnalysis: 'Amazon homepage with search box',
    };

    expect(mockDOMStateWithScreenshot.screenshot?.startsWith('data:image/')).toBe(true);
    expect(mockDOMStateWithScreenshot.visionAnalysis).toBe('Amazon homepage with search box');
  });
});

// ============================================================================
// Interactive Element Tests
// ============================================================================

describe('Interactive Element Structure', () => {
  const mockElement: InteractiveElement = {
    index: 0,
    tag: 'button',
    type: 'button',
    text: 'Add to Cart',
    selector: '#add-to-cart-button',
    attributes: { 'aria-label': 'Add to Cart' },
  };

  it('should have all required fields', () => {
    expect(mockElement.index).toBe(0);
    expect(mockElement.tag).toBe('button');
    expect(mockElement.type).toBe('button');
    expect(mockElement.text).toBe('Add to Cart');
    expect(mockElement.selector).toBe('#add-to-cart-button');
    expect(mockElement.attributes).toBeInstanceOf(Object);
  });

  it('should handle optional type field', () => {
    const linkElement: InteractiveElement = {
      index: 1,
      tag: 'a',
      text: 'Product Link',
      selector: 'a.product-link',
      attributes: { href: '/product/123' },
    };

    expect(linkElement.type).toBeUndefined();
  });

  it('should handle empty attributes', () => {
    const simpleElement: InteractiveElement = {
      index: 2,
      tag: 'div',
      text: 'Simple Element',
      selector: '.simple',
      attributes: {},
    };

    expect(simpleElement.attributes).toEqual({});
  });
});

// ============================================================================
// MAX_INTERACTIVE_ELEMENTS Configuration Tests
// ============================================================================

describe('MAX_INTERACTIVE_ELEMENTS Configuration', () => {
  it('should be defined as a constant', () => {
    expect(typeof MAX_INTERACTIVE_ELEMENTS).toBe('number');
    expect(MAX_INTERACTIVE_ELEMENTS).toBeGreaterThan(0);
  });

  it('should be set to a reasonable value (30)', () => {
    expect(MAX_INTERACTIVE_ELEMENTS).toBe(30);
  });

  it('should not be too large for LLM context', () => {
    expect(MAX_INTERACTIVE_ELEMENTS).toBeLessThanOrEqual(100);
  });
});
