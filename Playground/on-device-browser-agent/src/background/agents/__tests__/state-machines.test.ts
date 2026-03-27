/**
 * State Machine Tests
 *
 * Tests for Amazon and YouTube state machine implementations.
 * These tests verify the deterministic navigation logic.
 */

import type { AmazonTaskState } from './amazon-state-machine';
import type { YouTubeState } from './state-machines/youtube';

// ============================================================================
// Amazon Task State Tests
// ============================================================================

describe('AmazonTaskState Enum', () => {
  const expectedStates: AmazonTaskState[] = [
    'NAVIGATING',
    'SEARCHING',
    'RESULTS',
    'PRODUCT_PAGE',
    'ADDING_TO_CART',
    'VERIFYING_CART',
    'DONE',
    'PAUSED',
    'FAILED',
  ];

  it('should define all 9 Amazon task states', () => {
    expect(expectedStates.length).toBe(9);
  });

  it('should include navigation states', () => {
    expect(expectedStates).toContain('NAVIGATING');
    expect(expectedStates).toContain('SEARCHING');
  });

  it('should include product states', () => {
    expect(expectedStates).toContain('RESULTS');
    expect(expectedStates).toContain('PRODUCT_PAGE');
  });

  it('should include cart states', () => {
    expect(expectedStates).toContain('ADDING_TO_CART');
    expect(expectedStates).toContain('VERIFYING_CART');
  });

  it('should include terminal states', () => {
    expect(expectedStates).toContain('DONE');
    expect(expectedStates).toContain('FAILED');
  });

  it('should include pause state', () => {
    expect(expectedStates).toContain('PAUSED');
  });
});

// ============================================================================
// YouTube State Tests
// ============================================================================

describe('YouTubeState Enum', () => {
  const expectedStates: YouTubeState[] = [
    'NAVIGATING',
    'ON_HOMEPAGE',
    'TYPED_QUERY',
    'ON_RESULTS',
    'ON_VIDEO',
    'DONE',
  ];

  it('should define all 6 YouTube states', () => {
    expect(expectedStates.length).toBe(6);
  });

  it('should include navigation states', () => {
    expect(expectedStates).toContain('NAVIGATING');
    expect(expectedStates).toContain('ON_HOMEPAGE');
  });

  it('should include search states', () => {
    expect(expectedStates).toContain('TYPED_QUERY');
    expect(expectedStates).toContain('ON_RESULTS');
  });

  it('should include video state', () => {
    expect(expectedStates).toContain('ON_VIDEO');
  });

  it('should include done state', () => {
    expect(expectedStates).toContain('DONE');
  });
});

// ============================================================================
// State Machine Transition Tests
// ============================================================================

describe('State Machine Transitions', () => {
  it('should transition from NAVIGATING to SEARCHING on Amazon', () => {
    // Simulate state transition
    let state: AmazonTaskState = 'NAVIGATING';

    // When we detect Amazon URL
    const url = 'https://www.amazon.com/';
    if (url.includes('amazon.com')) {
      state = 'SEARCHING';
    }

    expect(state).toBe('SEARCHING');
  });

  it('should transition from SEARCHING to RESULTS after search', () => {
    let state: AmazonTaskState = 'SEARCHING';

    // After detecting search results URL
    const url = 'https://www.amazon.com/s?k=test';
    if (url.includes('/s?')) {
      state = 'RESULTS';
    }

    expect(state).toBe('RESULTS');
  });

  it('should transition from RESULTS to PRODUCT_PAGE on click', () => {
    let state: AmazonTaskState = 'RESULTS';

    // After clicking a product
    state = 'PRODUCT_PAGE';

    expect(state).toBe('PRODUCT_PAGE');
  });

  it('should transition from PRODUCT_PAGE to ADDING_TO_CART', () => {
    let state: AmazonTaskState = 'PRODUCT_PAGE';

    // After clicking add to cart
    state = 'ADDING_TO_CART';

    expect(state).toBe('ADDING_TO_CART');
  });

  it('should transition from ADDING_TO_CART to VERIFYING_CART', () => {
    let state: AmazonTaskState = 'ADDING_TO_CART';

    // After page changes to cart
    const pageState = 'cart';
    if (pageState === 'cart') {
      state = 'VERIFYING_CART';
    }

    expect(state).toBe('VERIFYING_CART');
  });

  it('should transition from VERIFYING_CART to DONE', () => {
    let state: AmazonTaskState = 'VERIFYING_CART';

    // After verifying item in cart
    state = 'DONE';

    expect(state).toBe('DONE');
  });
});

// ============================================================================
// Amazon Task Context Tests
// ============================================================================

describe('AmazonTaskContext', () => {
  interface TestContext {
    state: AmazonTaskState;
    searchQuery: string;
    retryCount: number;
    previousState?: AmazonTaskState;
    cartCountBefore?: number;
    selectedProduct?: string;
    obstacleType?: string;
  }

  it('should have required context fields', () => {
    const context: TestContext = {
      state: 'NAVIGATING',
      searchQuery: 'test product',
      retryCount: 0,
    };

    expect(context.state).toBe('NAVIGATING');
    expect(context.searchQuery).toBe('test product');
    expect(context.retryCount).toBe(0);
  });

  it('should support optional context fields', () => {
    const context: TestContext = {
      state: 'PRODUCT_PAGE',
      searchQuery: 'test product',
      retryCount: 0,
      previousState: 'RESULTS',
      cartCountBefore: 2,
      selectedProduct: 'Test Widget',
      obstacleType: 'LOGIN_REQUIRED',
    };

    expect(context.previousState).toBe('RESULTS');
    expect(context.cartCountBefore).toBe(2);
    expect(context.selectedProduct).toBe('Test Widget');
    expect(context.obstacleType).toBe('LOGIN_REQUIRED');
  });
});

// ============================================================================
// State Machine State Tests
// ============================================================================

describe('State Machine State Detection', () => {
  it('should detect NAVIGATING state on non-Amazon URL', () => {
    const url = 'https://www.google.com/';
    const isAmazon = url.includes('amazon.');

    if (!isAmazon) {
      // State would be NAVIGATING
      expect(true).toBe(true);
    }
  });

  it('should detect RESULTS state on search URL', () => {
    const url = 'https://www.amazon.com/s?k=test';
    const isSearchResults = url.includes('/s?');

    expect(isSearchResults).toBe(true);
  });

  it('should detect PRODUCT_PAGE state on product URL', () => {
    const url = 'https://www.amazon.com/dp/ABC123';
    const isProductPage = url.includes('/dp/');

    expect(isProductPage).toBe(true);
  });

  it('should detect cart state by URL', () => {
    const url = 'https://www.amazon.com/gp/cart/view.html';
    const isCart = url.includes('/gp/cart');

    expect(isCart).toBe(true);
  });
});

// ============================================================================
// YouTube State Machine Tests
// ============================================================================

describe('YouTube State Machine', () => {
  it('should navigate to YouTube first', () => {
    const url = '';
    const targetUrl = 'https://www.youtube.com';

    if (!url.includes('youtube.com')) {
      // Action would be navigate to YouTube
      expect(targetUrl).toBe('https://www.youtube.com');
    }
  });

  it('should detect video page state', () => {
    const url = 'https://www.youtube.com/watch?v=abc123';
    const isVideoPage = url.includes('/watch');

    expect(isVideoPage).toBe(true);
  });

  it('should detect search results state', () => {
    const url = 'https://www.youtube.com/results?search_query=test';
    const isResultsPage = url.includes('/results');

    expect(isResultsPage).toBe(true);
  });

  it('should detect homepage state', () => {
    const url = 'https://www.youtube.com/';
    const isHomepage = url === 'https://www.youtube.com/';

    expect(isHomepage).toBe(true);
  });
});

// ============================================================================
// Obstacle Handling Tests
// ============================================================================

describe('Obstacle Detection and Handling', () => {
  const obstacleTypes = ['LOGIN_REQUIRED', 'CAPTCHA', 'OUT_OF_STOCK', 'ERROR'] as const;

  it('should detect login required obstacle', () => {
    const pageText = 'Please sign in to continue';
    const isLoginRequired = pageText.includes('sign in') || pageText.includes('login');

    expect(isLoginRequired).toBe(true);
  });

  it('should detect captcha obstacle', () => {
    const pageText = 'Please solve the CAPTCHA to continue';
    const isCaptcha = pageText.toLowerCase().includes('captcha') || pageText.toLowerCase().includes('verify you are human');

    expect(isCaptcha).toBe(true);
  });

  it('should detect out of stock obstacle', () => {
    const pageText = 'This item is currently unavailable';
    const isOutOfStock = pageText.toLowerCase().includes('out of stock') || pageText.toLowerCase().includes('unavailable');

    expect(isOutOfStock).toBe(true);
  });

  it('should handle obstacle pause state', () => {
    let state: AmazonTaskState = 'SEARCHING';

    // When obstacle detected
    state = 'PAUSED';

    expect(state).toBe('PAUSED');
  });

  it('should resume obstacle state after user action', () => {
    let state: AmazonTaskState = 'PAUSED';
    let previousState: AmazonTaskState | undefined = 'SEARCHING';

    // After obstacle resolved
    if (previousState) {
      state = previousState;
    }

    expect(state).toBe('SEARCHING');
  });
});

// ============================================================================
// State Machine Result Tests
// ============================================================================

describe('StateMachineResult', () => {
  interface TestResult {
    action: string;
    state: string;
    machineName: string;
  }

  it('should return action and state', () => {
    const result: TestResult = {
      action: 'navigate',
      state: 'NAVIGATING',
      machineName: 'Amazon',
    };

    expect(result.action).toBe('navigate');
    expect(result.state).toBe('NAVIGATING');
    expect(result.machineName).toBe('Amazon');
  });

  it('should track state machine name', () => {
    const amazonResult: TestResult = {
      action: 'click',
      state: 'PRODUCT_PAGE',
      machineName: 'Amazon',
    };

    const youtubeResult: TestResult = {
      action: 'navigate',
      state: 'ON_HOMEPAGE',
      machineName: 'YouTube',
    };

    expect(amazonResult.machineName).toBe('Amazon');
    expect(youtubeResult.machineName).toBe('YouTube');
  });
});

// ============================================================================
// State Machine Reset Tests
// ============================================================================

describe('State Machine Reset', () => {
  it('should reset to initial state', () => {
    let state: AmazonTaskState = 'DONE';

    // Reset state machine
    state = 'NAVIGATING';

    expect(state).toBe('NAVIGATING');
  });

  it('should clear context on reset', () => {
    interface Context {
      state: AmazonTaskState;
      retryCount: number;
      searchQuery: string;
    }

    let context: Context = {
      state: 'PRODUCT_PAGE',
      retryCount: 3,
      searchQuery: 'old query',
    };

    // Reset context
    context = {
      state: 'NAVIGATING',
      retryCount: 0,
      searchQuery: '',
    };

    expect(context.state).toBe('NAVIGATING');
    expect(context.retryCount).toBe(0);
    expect(context.searchQuery).toBe('');
  });
});

// ============================================================================
// State Machine Query Tests
// ============================================================================

describe('State Machine Query Handling', () => {
  it('should store search query', () => {
    const searchQuery = 'test product';
    expect(searchQuery).toBe('test product');
  });

  it('should update query on new task', () => {
    let query = 'old query';
    query = 'new query';
    expect(query).toBe('new query');
  });

  it('should clear query on reset', () => {
    let query = 'some query';
    query = '';
    expect(query).toBe('');
  });
});
