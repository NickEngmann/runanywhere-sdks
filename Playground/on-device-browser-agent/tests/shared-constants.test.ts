/**
 * Shared Constants Tests
 * 
 * Tests for shared constants including:
 * - LLM model configurations
 * - Agent configuration
 * - DOM observation configuration
 * - Amazon-specific configuration
 * - Timing configuration
 */

import { describe, it, expect } from '@jest/globals';
import * as constants from '../src/shared/constants';

// ============================================================================
// Model Configuration Tests
// ============================================================================

describe('Model Configuration', () => {
  it('should have LLM_ENGINE_TYPE defined', () => {
    expect(constants.LLM_ENGINE_TYPE).toBe('webllm');
  });

  it('should have DEFAULT_MODEL defined', () => {
    expect(constants.DEFAULT_MODEL).toBeDefined();
    expect(typeof constants.DEFAULT_MODEL).toBe('string');
    expect(constants.DEFAULT_MODEL.length).toBeGreaterThan(0);
  });

  it('should have AVAILABLE_LLM_MODELS array', () => {
    expect(Array.isArray(constants.AVAILABLE_LLM_MODELS)).toBe(true);
    expect(constants.AVAILABLE_LLM_MODELS.length).toBeGreaterThan(0);

    // Check each model has required fields
    constants.AVAILABLE_LLM_MODELS.forEach((model) => {
      expect(model).toHaveProperty('id');
      expect(model).toHaveProperty('name');
      expect(model).toHaveProperty('size');
      expect(model).toHaveProperty('engine');
      expect(typeof model.id).toBe('string');
      expect(typeof model.name).toBe('string');
      expect(typeof model.size).toBe('string');
      expect(['webllm', 'transformers']).toContain(model.engine);
    });
  });

  it('should have AVAILABLE_VLM_MODELS array', () => {
    expect(Array.isArray(constants.AVAILABLE_VLM_MODELS)).toBe(true);
    expect(constants.AVAILABLE_VLM_MODELS.length).toBeGreaterThan(0);

    // Check each model has required fields
    constants.AVAILABLE_VLM_MODELS.forEach((model) => {
      expect(model).toHaveProperty('id');
      expect(model).toHaveProperty('name');
      expect(model).toHaveProperty('size');
      expect(typeof model.id).toBe('string');
      expect(model.id).toMatch(/^tiny|small|base$/);
    });
  });

  it('should have FALLBACK_MODELS array', () => {
    expect(Array.isArray(constants.FALLBACK_MODELS)).toBe(true);
    expect(constants.FALLBACK_MODELS.length).toBeGreaterThan(0);
  });
});

// ============================================================================
// Agent Configuration Tests
// ============================================================================

describe('Agent Configuration', () => {
  it('should have MAX_STEPS defined', () => {
    expect(constants.MAX_STEPS).toBeGreaterThan(0);
    expect(typeof constants.MAX_STEPS).toBe('number');
  });

  it('should have MAX_REPLANS defined', () => {
    expect(constants.MAX_REPLANS).toBeGreaterThanOrEqual(0);
    expect(typeof constants.MAX_REPLANS).toBe('number');
  });

  it('should have AGENT_TEMPERATURE defined', () => {
    expect(constants.AGENT_TEMPERATURE).toBeGreaterThanOrEqual(0);
    expect(constants.AGENT_TEMPERATURE).toBeLessThanOrEqual(1);
    expect(typeof constants.AGENT_TEMPERATURE).toBe('number');
  });

  it('should have AGENT_MAX_TOKENS defined', () => {
    expect(constants.AGENT_MAX_TOKENS).toBeGreaterThan(0);
    expect(typeof constants.AGENT_MAX_TOKENS).toBe('number');
  });

  it('should have MAX_LLM_CALLS_PER_TASK defined', () => {
    expect(constants.MAX_LLM_CALLS_PER_TASK).toBeGreaterThan(0);
    expect(typeof constants.MAX_LLM_CALLS_PER_TASK).toBe('number');
  });
});

// ============================================================================
// DOM Observation Configuration Tests
// ============================================================================

describe('DOM Observation Configuration', () => {
  it('should have INTERACTIVE_SELECTORS array', () => {
    expect(Array.isArray(constants.INTERACTIVE_SELECTORS)).toBe(true);
    expect(constants.INTERACTIVE_SELECTORS.length).toBeGreaterThan(0);

    // Check each selector is a valid CSS selector
    constants.INTERACTIVE_SELECTORS.forEach((selector) => {
      expect(typeof selector).toBe('string');
      expect(selector.length).toBeGreaterThan(0);
    });
  });

  it('should have MAX_INTERACTIVE_ELEMENTS defined', () => {
    expect(constants.MAX_INTERACTIVE_ELEMENTS).toBeGreaterThan(0);
    expect(typeof constants.MAX_INTERACTIVE_ELEMENTS).toBe('number');
  });

  it('should have MAX_PAGE_TEXT_LENGTH defined', () => {
    expect(constants.MAX_PAGE_TEXT_LENGTH).toBeGreaterThan(0);
    expect(typeof constants.MAX_PAGE_TEXT_LENGTH).toBe('number');
  });
});

// ============================================================================
// Amazon-Specific Configuration Tests
// ============================================================================

describe('Amazon-Specific Configuration', () => {
  describe('URL Patterns', () => {
    it('should have AMAZON_URL_PATTERNS object', () => {
      expect(constants.AMAZON_URL_PATTERNS).toBeDefined();
      expect(typeof constants.AMAZON_URL_PATTERNS).toBe('object');
    });

    it('should have homepage pattern', () => {
      expect(constants.AMAZON_URL_PATTERNS.homepage).toBeInstanceOf(RegExp);
      expect(constants.AMAZON_URL_PATTERNS.homepage.test('https://www.amazon.com/')).toBe(true);
    });

    it('should have search pattern', () => {
      expect(constants.AMAZON_URL_PATTERNS.search).toBeInstanceOf(RegExp);
      expect(constants.AMAZON_URL_PATTERNS.search.test('https://www.amazon.com/s?k=phone')).toBe(true);
    });

    it('should have product pattern', () => {
      expect(constants.AMAZON_URL_PATTERNS.product).toBeInstanceOf(RegExp);
      expect(constants.AMAZON_URL_PATTERNS.product.test('https://www.amazon.com/dp/ABC123')).toBe(true);
    });

    it('should have cart pattern', () => {
      expect(constants.AMAZON_URL_PATTERNS.cart).toBeInstanceOf(RegExp);
      expect(constants.AMAZON_URL_PATTERNS.cart.test('https://www.amazon.com/gp/cart/view.html')).toBe(true);
    });

    it('should have signin pattern', () => {
      expect(constants.AMAZON_URL_PATTERNS.signin).toBeInstanceOf(RegExp);
      expect(constants.AMAZON_URL_PATTERNS.signin.test('https://www.amazon.com/ap/signin')).toBe(true);
    });

    it('should have checkout pattern', () => {
      expect(constants.AMAZON_URL_PATTERNS.checkout).toBeInstanceOf(RegExp);
      expect(constants.AMAZON_URL_PATTERNS.checkout.test('https://www.amazon.com/gp/buy')).toBe(true);
    });
  });

  describe('Selectors', () => {
    it('should have AMAZON_SELECTORS object', () => {
      expect(constants.AMAZON_SELECTORS).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS).toBe('object');
    });

    it('should have search input selector', () => {
      expect(constants.AMAZON_SELECTORS.searchInput).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS.searchInput).toBe('string');
      expect(constants.AMAZON_SELECTORS.searchInput.length).toBeGreaterThan(0);
    });

    it('should have search button selector', () => {
      expect(constants.AMAZON_SELECTORS.searchButton).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS.searchButton).toBe('string');
    });

    it('should have product card selector', () => {
      expect(constants.AMAZON_SELECTORS.productCard).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS.productCard).toBe('string');
    });

    it('should have add to cart button selector', () => {
      expect(constants.AMAZON_SELECTORS.addToCartButton).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS.addToCartButton).toBe('string');
    });

    it('should have buy now button selector', () => {
      expect(constants.AMAZON_SELECTORS.buyNowButton).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS.buyNowButton).toBe('string');
    });

    it('should have cart count selector', () => {
      expect(constants.AMAZON_SELECTORS.cartCount).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS.cartCount).toBe('string');
    });

    it('should have captcha form selector', () => {
      expect(constants.AMAZON_SELECTORS.captchaForm).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS.captchaForm).toBe('string');
    });

    it('should have signin form selector', () => {
      expect(constants.AMAZON_SELECTORS.signinForm).toBeDefined();
      expect(typeof constants.AMAZON_SELECTORS.signinForm).toBe('string');
    });
  });

  describe('Success Patterns', () => {
    it('should have AMAZON_SUCCESS_PATTERNS object', () => {
      expect(constants.AMAZON_SUCCESS_PATTERNS).toBeDefined();
      expect(typeof constants.AMAZON_SUCCESS_PATTERNS).toBe('object');
    });

    it('should have addedToCart patterns array', () => {
      expect(constants.AMAZON_SUCCESS_PATTERNS.addedToCart).toBeDefined();
      expect(Array.isArray(constants.AMAZON_SUCCESS_PATTERNS.addedToCart)).toBe(true);
      expect(constants.AMAZON_SUCCESS_PATTERNS.addedToCart.length).toBeGreaterThan(0);
    });

    it('should have searchResults patterns array', () => {
      expect(constants.AMAZON_SUCCESS_PATTERNS.searchResults).toBeDefined();
      expect(Array.isArray(constants.AMAZON_SUCCESS_PATTERNS.searchResults)).toBe(true);
    });

    it('should have productPage patterns array', () => {
      expect(constants.AMAZON_SUCCESS_PATTERNS.productPage).toBeDefined();
      expect(Array.isArray(constants.AMAZON_SUCCESS_PATTERNS.productPage)).toBe(true);
    });
  });

  describe('Obstacle Patterns', () => {
    it('should have AMAZON_OBSTACLE_PATTERNS object', () => {
      expect(constants.AMAZON_OBSTACLE_PATTERNS).toBeDefined();
      expect(typeof constants.AMAZON_OBSTACLE_PATTERNS).toBe('object');
    });

    it('should have login patterns', () => {
      expect(constants.AMAZON_OBSTACLE_PATTERNS.login).toBeDefined();
      expect(Array.isArray(constants.AMAZON_OBSTACLE_PATTERNS.login)).toBe(true);
    });

    it('should have captcha patterns', () => {
      expect(constants.AMAZON_OBSTACLE_PATTERNS.captcha).toBeDefined();
      expect(Array.isArray(constants.AMAZON_OBSTACLE_PATTERNS.captcha)).toBe(true);
    });

    it('should have outOfStock patterns', () => {
      expect(constants.AMAZON_OBSTACLE_PATTERNS.outOfStock).toBeDefined();
      expect(Array.isArray(constants.AMAZON_OBSTACLE_PATTERNS.outOfStock)).toBe(true);
    });

    it('should have priceChange patterns', () => {
      expect(constants.AMAZON_OBSTACLE_PATTERNS.priceChange).toBeDefined();
      expect(Array.isArray(constants.AMAZON_OBSTACLE_PATTERNS.priceChange)).toBe(true);
    });
  });
});

// ============================================================================
// Timing Configuration Tests
// ============================================================================

describe('Timing Configuration', () => {
  it('should have POST_NAVIGATION_DELAY defined', () => {
    expect(constants.POST_NAVIGATION_DELAY).toBeGreaterThan(0);
    expect(typeof constants.POST_NAVIGATION_DELAY).toBe('number');
  });

  it('should have TYPING_DELAY defined', () => {
    expect(constants.TYPING_DELAY).toBeGreaterThan(0);
    expect(typeof constants.TYPING_DELAY).toBe('number');
  });

  it('should have DEFAULT_WAIT_TIMEOUT defined', () => {
    expect(constants.DEFAULT_WAIT_TIMEOUT).toBeGreaterThan(0);
    expect(typeof constants.DEFAULT_WAIT_TIMEOUT).toBe('number');
  });

  it('should have PAGE_LOAD_TIMEOUT defined', () => {
    expect(constants.PAGE_LOAD_TIMEOUT).toBeGreaterThan(0);
    expect(typeof constants.PAGE_LOAD_TIMEOUT).toBe('number');
  });
});

// ============================================================================
// Message Port Names Tests
// ============================================================================

describe('Message Port Names', () => {
  it('should have POPUP_PORT_NAME defined', () => {
    expect(constants.POPUP_PORT_NAME).toBeDefined();
    expect(typeof constants.POPUP_PORT_NAME).toBe('string');
    expect(constants.POPUP_PORT_NAME.length).toBeGreaterThan(0);
  });
});
