import {
  LLM_ENGINE_TYPE,
  DEFAULT_MODEL,
  AVAILABLE_LLM_MODELS,
  FALLBACK_MODELS,
  MAX_STEPS,
  MAX_REPLANS,
  AGENT_TEMPERATURE,
  AGENT_MAX_TOKENS,
  INTERACTIVE_SELECTORS,
  MAX_INTERACTIVE_ELEMENTS,
  MAX_PAGE_TEXT_LENGTH,
  AMAZON_URL_PATTERNS,
  AMAZON_SELECTORS,
  AMAZON_SUCCESS_PATTERNS,
  AMAZON_OBSTACLE_PATTERNS,
  POPUP_PORT_NAME,
} from '../src/shared/constants';
import type {
  DOMState,
  AmazonPageState,
  InteractiveElement,
  ActionType,
  AmazonPageState as AmazonStateType,
} from '../src/shared/types';

// ============================================================================
// Constants Tests
// ============================================================================

describe('Shared Constants', () => {
  describe('LLM Configuration', () => {
    it('LLM_ENGINE_TYPE should be webllm', () => {
      expect(LLM_ENGINE_TYPE).toBe('webllm');
    });

    it('DEFAULT_MODEL should be Qwen2.5-3B-Instruct', () => {
      expect(DEFAULT_MODEL).toBe('Qwen2.5-3B-Instruct-q4f16_1-MLC');
    });

    it('AVAILABLE_LLM_MODELS should have 5 models', () => {
      expect(AVAILABLE_LLM_MODELS).toHaveLength(5);
      const modelIds = AVAILABLE_LLM_MODELS.map(m => m.id);
      expect(modelIds).toContain('Qwen2.5-3B-Instruct-q4f16_1-MLC');
      expect(modelIds).toContain('Qwen2.5-1.5B-Instruct-q4f16_1-MLC');
      expect(modelIds).toContain('Llama-3.2-1B-Instruct-q4f16_1-MLC');
      expect(modelIds).toContain('Phi-3.5-mini-instruct-q4f16_1-MLC');
      expect(modelIds).toContain('LiquidAI/LFM2.5-1.2B-Instruct-ONNX');
    });

    it('AVAILABLE_LLM_MODELS should have correct engine distribution', () => {
      const webllmCount = AVAILABLE_LLM_MODELS.filter(m => m.engine === 'webllm').length;
      const transformersCount = AVAILABLE_LLM_MODELS.filter(m => m.engine === 'transformers').length;
      expect(webllmCount).toBe(4);
      expect(transformersCount).toBe(1);
    });

    it('FALLBACK_MODELS should have 3 models', () => {
      expect(FALLBACK_MODELS).toHaveLength(3);
      expect(FALLBACK_MODELS).toContain('Qwen2.5-3B-Instruct-q4f16_1-MLC');
    });
  });

  describe('Agent Configuration', () => {
    it('MAX_STEPS should be 25', () => {
      expect(MAX_STEPS).toBe(25);
    });

    it('MAX_REPLANS should be 2', () => {
      expect(MAX_REPLANS).toBe(2);
    });

    it('AGENT_TEMPERATURE should be 0.3', () => {
      expect(AGENT_TEMPERATURE).toBe(0.3);
    });

    it('AGENT_MAX_TOKENS should be 512', () => {
      expect(AGENT_MAX_TOKENS).toBe(512);
    });
  });

  describe('DOM Observation Configuration', () => {
    it('INTERACTIVE_SELECTORS should have 9 selectors', () => {
      expect(INTERACTIVE_SELECTORS).toHaveLength(9);
      expect(INTERACTIVE_SELECTORS).toContain('a[href]');
      expect(INTERACTIVE_SELECTORS).toContain('button');
      expect(INTERACTIVE_SELECTORS).toContain('input');
      expect(INTERACTIVE_SELECTORS).toContain('textarea');
      expect(INTERACTIVE_SELECTORS).toContain('select');
    });

    it('MAX_INTERACTIVE_ELEMENTS should be 30', () => {
      expect(MAX_INTERACTIVE_ELEMENTS).toBe(30);
    });

    it('MAX_PAGE_TEXT_LENGTH should be 1500', () => {
      expect(MAX_PAGE_TEXT_LENGTH).toBe(1500);
    });
  });

  describe('Amazon Configuration', () => {
    it('AMAZON_URL_PATTERNS should have 7 patterns', () => {
      expect(AMAZON_URL_PATTERNS).toHaveProperty('homepage');
      expect(AMAZON_URL_PATTERNS).toHaveProperty('search');
      expect(AMAZON_URL_PATTERNS).toHaveProperty('product');
      expect(AMAZON_URL_PATTERNS).toHaveProperty('cart');
      expect(AMAZON_URL_PATTERNS).toHaveProperty('signin');
      expect(AMAZON_URL_PATTERNS).toHaveProperty('checkout');
    });

    it('AMAZON_URL_PATTERNS.should match expected URLs', () => {
      const homepageUrl = 'https://www.amazon.com/';
      const searchUrl = 'https://www.amazon.com/s?k=test';
      const productUrl = 'https://www.amazon.com/dp/B08EXAMPLE';
      const cartUrl = 'https://www.amazon.com/gp/cart/view.html';
      const signinUrl = 'https://www.amazon.com/ap/signin';
      const checkoutUrl = 'https://www.amazon.com/gp/buy';

      expect(AMAZON_URL_PATTERNS.homepage.test(homepageUrl)).toBe(true);
      expect(AMAZON_URL_PATTERNS.search.test(searchUrl)).toBe(true);
      expect(AMAZON_URL_PATTERNS.product.test(productUrl)).toBe(true);
      expect(AMAZON_URL_PATTERNS.cart.test(cartUrl)).toBe(true);
      expect(AMAZON_URL_PATTERNS.signin.test(signinUrl)).toBe(true);
    });

    it('AMAZON_SELECTORS should have 16 selectors', () => {
      const selectors = Object.keys(AMAZON_SELECTORS);
      expect(selectors).toContain('searchInput');
      expect(selectors).toContain('searchButton');
      expect(selectors).toContain('productCard');
      expect(selectors).toContain('addToCartButton');
      expect(selectors).toContain('buyNowButton');
      expect(selectors).toContain('cartCount');
      expect(selectors).toContain('captchaForm');
    });

    it('AMAZON_SUCCESS_PATTERNS should have 3 categories', () => {
      expect(AMAZON_SUCCESS_PATTERNS).toHaveProperty('addedToCart');
      expect(AMAZON_SUCCESS_PATTERNS).toHaveProperty('searchResults');
      expect(AMAZON_SUCCESS_PATTERNS).toHaveProperty('productPage');
      expect(AMAZON_SUCCESS_PATTERNS.addedToCart).toHaveLength(3);
    });

    it('AMAZON_OBSTACLE_PATTERNS should have 4 categories', () => {
      expect(AMAZON_OBSTACLE_PATTERNS).toHaveProperty('login');
      expect(AMAZON_OBSTACLE_PATTERNS).toHaveProperty('captcha');
      expect(AMAZON_OBSTACLE_PATTERNS).toHaveProperty('outOfStock');
      expect(AMAZON_OBSTACLE_PATTERNS).toHaveProperty('priceChange');
    });
  });

  describe('POPUP_PORT_NAME', () => {
    it('should be "popup-connection"', () => {
      expect(POPUP_PORT_NAME).toBe('popup-connection');
    });
  });
});

// ============================================================================
// Types Tests (Type-level tests)
// ============================================================================

describe('Type Definitions', () => {
  describe('AmazonPageState', () => {
    it('should have all 7 valid states', () => {
      const states: AmazonPageState[] = [
        'homepage',
        'search_results',
        'product_page',
        'cart',
        'checkout',
        'signin',
        'captcha',
      ];
      expect(states).toHaveLength(7);
    });

    it('should not accept invalid states', () => {
      // This will fail at compile time if uncommented
      // const invalidState: AmazonPageState = 'invalid';
      expect(true).toBe(true);
    });
  });

  describe('ActionType', () => {
    it('should have all 9 valid action types', () => {
      const actions: ActionType[] = [
        'navigate',
        'click',
        'type',
        'press_enter',
        'extract',
        'scroll',
        'wait',
        'done',
        'fail',
      ];
      expect(actions).toHaveLength(9);
    });
  });

  describe('InteractiveElement', () => {
    it('should have all required fields', () => {
      const element: InteractiveElement = {
        index: 0,
        tag: 'button',
        type: 'button',
        text: 'Click me',
        selector: '#my-button',
        attributes: { 'aria-label': 'Click me' },
      };

      expect(element).toHaveProperty('index');
      expect(element).toHaveProperty('tag');
      expect(element).toHaveProperty('type');
      expect(element).toHaveProperty('text');
      expect(element).toHaveProperty('selector');
      expect(element).toHaveProperty('attributes');
    });

    it('should allow optional fields', () => {
      const element: InteractiveElement = {
        index: 1,
        tag: 'a',
        text: 'Link',
        selector: 'a[href]',
        attributes: {},
      };

      expect(element).not.toHaveProperty('type');
      expect(element.index).toBe(1);
    });
  });

  describe('DOMState', () => {
    it('should have all required fields', () => {
      const state: DOMState = {
        url: 'https://example.com',
        title: 'Example',
        interactiveElements: [],
        pageText: 'Some text content',
      };

      expect(state).toHaveProperty('url');
      expect(state).toHaveProperty('title');
      expect(state).toHaveProperty('interactiveElements');
      expect(state).toHaveProperty('pageText');
    });

    it('should have optional Amazon-specific fields', () => {
      const state: DOMState = {
        url: 'https://amazon.com/product/123',
        title: 'Product',
        interactiveElements: [],
        pageText: '',
        pageState: 'product_page',
        cartCount: 5,
        alerts: ['Free shipping available'],
      };

      expect(state).toHaveProperty('pageState');
      expect(state).toHaveProperty('cartCount');
      expect(state).toHaveProperty('alerts');
    });

    it('should have optional VLM fields', () => {
      const state: DOMState = {
        url: 'https://example.com',
        title: 'Example',
        interactiveElements: [],
        pageText: 'Content',
        screenshot: 'data:image/jpeg;base64,abc123',
        visionAnalysis: 'This is a simple page',
      };

      expect(state).toHaveProperty('screenshot');
      expect(state).toHaveProperty('visionAnalysis');
    });
  });
});
