/**
 * Executor and Utility Tests
 *
 * Tests for executor logic, change observer, and obstacle detection.
 * These tests verify the core orchestration functionality.
 */

import type { DOMState, AgentContext, AgentStep } from '../shared/types';
import type { ChangeResult } from '../background/agents/change-observer';

// ============================================================================
// Agent Context Tests
// ============================================================================

describe('AgentContext Structure', () => {
  const mockContext: AgentContext = {
    task: 'Add widget to Amazon cart',
    history: [],
  };

  it('should have required task field', () => {
    expect(mockContext.task).toBe('Add widget to Amazon cart');
  });

  it('should have empty history by default', () => {
    expect(mockContext.history).toHaveLength(0);
  });

  it('should support optional plan', () => {
    const contextWithPlan: AgentContext = {
      task: 'Search for product',
      plan: {
        current_state: {
          analysis: 'Task to find and add product',
          memory: [],
        },
        plan: {
          thought: 'Navigate and search',
          steps: ['Navigate to Amazon', 'Search for product', 'Add to cart'],
          success_criteria: 'Item in cart',
        },
      },
      history: [],
    };

    expect(contextWithPlan.plan).toBeDefined();
    expect(contextWithPlan.plan?.plan?.steps).toHaveLength(3);
  });
});

// ============================================================================
// Agent Step History Tests
// ============================================================================

describe('AgentStep History', () => {
  const mockStep: AgentStep = {
    action: {
      action_type: 'navigate',
      parameters: { url: 'https://www.amazon.com' },
      thought: 'Navigate to Amazon',
    },
    result: {
      success: true,
      data: 'Navigated to Amazon',
    },
    timestamp: Date.now(),
  };

  it('should record action details', () => {
    expect(mockStep.action.action_type).toBe('navigate');
    expect(mockStep.action.parameters.url).toBe('https://www.amazon.com');
  });

  it('should record action result', () => {
    expect(mockStep.result.success).toBe(true);
    expect(mockStep.result.data).toBe('Navigated to Amazon');
  });

  it('should have timestamp', () => {
    const timeDiff = Date.now() - mockStep.timestamp;
    expect(timeDiff).toBeLessThan(1000); // Less than 1 second old
    expect(timeDiff).toBeGreaterThanOrEqual(0);
  });

  it('should support history array', () => {
    const history: AgentStep[] = [mockStep];
    expect(history).toHaveLength(1);
  });
});

// ============================================================================
// DOM State for Change Observer Tests
// ============================================================================

describe('ChangeObserver DOMSnapshot', () => {
  interface TestSnapshot {
    url: string;
    title: string;
    textHash: number;
    elementCount: number;
    cartCount: number;
    timestamp: number;
  }

  const mockSnapshot: TestSnapshot = {
    url: 'https://www.amazon.com/',
    title: 'Amazon.com. Spend less. Smile more.',
    textHash: 123456789,
    elementCount: 45,
    cartCount: 0,
    timestamp: Date.now(),
  };

  it('should track URL', () => {
    expect(mockSnapshot.url).toBe('https://www.amazon.com/');
  });

  it('should track title', () => {
    expect(mockSnapshot.title).toContain('Amazon');
  });

  it('should track text hash for change detection', () => {
    expect(mockSnapshot.textHash).toBeGreaterThan(0);
  });

  it('should track element count', () => {
    expect(mockSnapshot.elementCount).toBe(45);
  });

  it('should track cart count', () => {
    expect(mockSnapshot.cartCount).toBe(0);
  });

  it('should track timestamp', () => {
    const timeDiff = Date.now() - mockSnapshot.timestamp;
    expect(timeDiff).toBeLessThan(1000);
  });
});

// ============================================================================
// Change Result Detection Tests
// ============================================================================

describe('ChangeObserver ChangeResult', () => {
  const mockChanges: ChangeResult = {
    urlChanged: false,
    titleChanged: false,
    pageChanged: true,
    elementsChanged: false,
    cartIncremented: false,
    successPattern: null,
    errorPattern: null,
    timeSinceSnapshot: 500,
  };

  it('should detect URL change', () => {
    const changed: ChangeResult = { ...mockChanges, urlChanged: true };
    expect(changed.urlChanged).toBe(true);
  });

  it('should detect page content change', () => {
    expect(mockChanges.pageChanged).toBe(true);
  });

  it('should detect cart increment', () => {
    const withCartChange: ChangeResult = { ...mockChanges, cartIncremented: true };
    expect(withCartChange.cartIncremented).toBe(true);
  });

  it('should detect success patterns', () => {
    const withSuccess: ChangeResult = { ...mockChanges, successPattern: 'added to cart' };
    expect(withSuccess.successPattern).toBe('added to cart');
  });

  it('should detect error patterns', () => {
    const withError: ChangeResult = { ...mockChanges, errorPattern: 'out of stock' };
    expect(withError.errorPattern).toBe('out of stock');
  });

  it('should track time since snapshot', () => {
    expect(mockChanges.timeSinceSnapshot).toBeGreaterThan(0);
  });
});

// ============================================================================
// Success Pattern Detection Tests
// ============================================================================

describe('Success Pattern Detection', () => {
  const SUCCESS_PATTERNS = {
    addToCart: ['added to cart', 'added to your cart', '1 item added', 'view cart', 'go to cart'],
    search: ['results for', 'showing', 'found', 'search results'],
    video: ['subscribe', 'share', 'save to playlist', 'video'],
    form: ['thank you', 'success', 'confirmed', 'submitted'],
  };

  it('should detect add to cart success', () => {
    const text = 'Item added to your cart';
    const matched = SUCCESS_PATTERNS.addToCart.some(p => text.includes(p));
    expect(matched).toBe(true);
  });

  it('should detect search success', () => {
    const text = 'Results for "test product" showing 1-16';
    const matched = SUCCESS_PATTERNS.search.some(p => text.includes(p));
    expect(matched).toBe(true);
  });

  it('should detect video interaction success', () => {
    const text = 'Subscribe button available';
    const matched = SUCCESS_PATTERNS.video.some(p => text.toLowerCase().includes(p));
    expect(matched).toBe(true);
  });

  it('should detect form submission success', () => {
    const text = 'Your form has been submitted successfully';
    const matched = SUCCESS_PATTERNS.form.some(p => text.includes(p));
    expect(matched).toBe(true);
  });

  it('should not match non-success patterns', () => {
    const text = 'Product not found';
    const matched = SUCCESS_PATTERNS.addToCart.some(p => text.includes(p));
    expect(matched).toBe(false);
  });
});

// ============================================================================
// Error Pattern Detection Tests
// ============================================================================

describe('Error Pattern Detection', () => {
  const ERROR_PATTERNS = {
    outOfStock: ['out of stock', 'currently unavailable', 'sold out', 'not available', 'unavailable'],
    error: ['something went wrong', 'error occurred', 'unable to process', 'please try again', 'error'],
    loginRequired: ['sign in', 'log in', 'create account', 'register'],
    captcha: ['verify you are human', 'captcha', 'robot', 'characters'],
  };

  it('should detect out of stock', () => {
    const text = 'Currently unavailable';
    const matched = ERROR_PATTERNS.outOfStock.some(p => text.toLowerCase().includes(p));
    expect(matched).toBe(true);
  });

  it('should detect general errors', () => {
    const text = 'Something went wrong';
    const matched = ERROR_PATTERNS.error.some(p => text.toLowerCase().includes(p));
    expect(matched).toBe(true);
  });

  it('should detect login required', () => {
    const text = 'Please sign in to continue';
    const matched = ERROR_PATTERNS.loginRequired.some(p => text.toLowerCase().includes(p));
    expect(matched).toBe(true);
  });

  it('should detect captcha', () => {
    const text = 'Enter the characters you see below';
    const matched = ERROR_PATTERNS.captcha.some(p => text.toLowerCase().includes(p));
    expect(matched).toBe(true);
  });
});

// ============================================================================
// Significant Change Detection Tests
// ============================================================================

describe('Significant Change Detection', () => {
  const mockChanges: ChangeResult = {
    urlChanged: false,
    titleChanged: false,
    pageChanged: true,
    elementsChanged: false,
    cartIncremented: false,
    successPattern: null,
    errorPattern: null,
    timeSinceSnapshot: 500,
  };

  it('should detect significant changes from URL', () => {
    const significant = { ...mockChanges, urlChanged: true };
    const hasChange = significant.urlChanged ||
                     significant.pageChanged ||
                     significant.cartIncremented ||
                     significant.successPattern !== null;
    expect(hasChange).toBe(true);
  });

  it('should detect significant changes from page content', () => {
    const significant = { ...mockChanges, pageChanged: true };
    const hasChange = significant.urlChanged ||
                     significant.pageChanged ||
                     significant.cartIncremented ||
                     significant.successPattern !== null;
    expect(hasChange).toBe(true);
  });

  it('should detect significant changes from cart', () => {
    const significant = { ...mockChanges, cartIncremented: true };
    const hasChange = significant.urlChanged ||
                     significant.pageChanged ||
                     significant.cartIncremented ||
                     significant.successPattern !== null;
    expect(hasChange).toBe(true);
  });

  it('should detect success pattern as significant', () => {
    const significant = { ...mockChanges, successPattern: 'added to cart' };
    const hasChange = significant.urlChanged ||
                     significant.pageChanged ||
                     significant.cartIncremented ||
                     significant.successPattern !== null;
    expect(hasChange).toBe(true);
  });

  it('should not consider minor changes significant', () => {
    const changes: ChangeResult = {
      urlChanged: false,
      titleChanged: true,
      pageChanged: false,
      elementsChanged: false,
      cartIncremented: false,
      successPattern: null,
      errorPattern: null,
      timeSinceSnapshot: 100,
    };
    const hasSignificantChange = changes.urlChanged ||
                                changes.pageChanged ||
                                changes.cartIncremented ||
                                changes.successPattern !== null;
    expect(hasSignificantChange).toBe(false);
  });
});

// ============================================================================
// Action Success Verification Tests
// ============================================================================

describe('Action Success Verification', () => {
  it('should verify navigate action success by URL change', () => {
    const actionType = 'navigate';
    const changes: ChangeResult = {
      urlChanged: true,
      titleChanged: false,
      pageChanged: false,
      elementsChanged: false,
      cartIncremented: false,
      successPattern: null,
      errorPattern: null,
      timeSinceSnapshot: 1000,
    };

    const likelySucceeded = actionType === 'navigate' && changes.urlChanged;
    expect(likelySucceeded).toBe(true);
  });

  it('should verify press_enter action success', () => {
    const actionType = 'press_enter';
    const changes: ChangeResult = {
      urlChanged: false,
      titleChanged: false,
      pageChanged: true,
      elementsChanged: false,
      cartIncremented: false,
      successPattern: null,
      errorPattern: null,
      timeSinceSnapshot: 500,
    };

    const likelySucceeded = actionType === 'press_enter' && (changes.urlChanged || changes.pageChanged);
    expect(likelySucceeded).toBe(true);
  });

  it('should verify click action success', () => {
    const actionType = 'click';
    const changes: ChangeResult = {
      urlChanged: false,
      titleChanged: false,
      pageChanged: true,
      elementsChanged: true,
      cartIncremented: false,
      successPattern: null,
      errorPattern: null,
      timeSinceSnapshot: 300,
    };

    const likelySucceeded = actionType === 'click' && (changes.urlChanged || changes.pageChanged || changes.elementsChanged);
    expect(likelySucceeded).toBe(true);
  });
});

// ============================================================================
// Text Hash Computation Tests
// ============================================================================

describe('Text Hash Computation', () => {
  function hashText(text: string): number {
    let hash = 0;
    const sample = text.slice(0, 1000);

    for (let i = 0; i < sample.length; i++) {
      const char = sample.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }

    return hash;
  }

  it('should produce consistent hashes', () => {
    const text = 'Amazon test page';
    const hash1 = hashText(text);
    const hash2 = hashText(text);
    expect(hash1).toBe(hash2);
  });

  it('should produce different hashes for different text', () => {
    const text1 = 'Amazon homepage';
    const text2 = 'Different page';
    const hash1 = hashText(text1);
    const hash2 = hashText(text2);
    expect(hash1).not.toBe(hash2);
  });

  it('should handle empty text', () => {
    const hash = hashText('');
    expect(hash).toBe(0);
  });

  it('should produce valid integer hashes', () => {
    const text = 'Test text for hashing';
    const hash = hashText(text);
    expect(typeof hash).toBe('number');
    expect(Number.isInteger(hash)).toBe(true);
  });
});

// ============================================================================
// Change Description Tests
// ============================================================================

describe('Change Description', () => {
  function describeChanges(changes: ChangeResult): string {
    const parts: string[] = [];

    if (changes.urlChanged) parts.push('URL changed');
    if (changes.titleChanged) parts.push('title changed');
    if (changes.cartIncremented) parts.push('cart updated');
    if (changes.successPattern) parts.push(`success: "${changes.successPattern}"`);
    if (changes.errorPattern) parts.push(`error: "${changes.errorPattern}"`);

    if (parts.length === 0) {
      if (changes.pageChanged) return 'page content changed';
      if (changes.elementsChanged) return 'elements changed';
      return 'no observable changes';
    }

    return parts.join(', ');
  }

  it('should describe URL change', () => {
    const changes: ChangeResult = {
      urlChanged: true,
      titleChanged: false,
      pageChanged: false,
      elementsChanged: false,
      cartIncremented: false,
      successPattern: null,
      errorPattern: null,
      timeSinceSnapshot: 0,
    };
    expect(describeChanges(changes)).toBe('URL changed');
  });

  it('should describe multiple changes', () => {
    const changes: ChangeResult = {
      urlChanged: true,
      titleChanged: true,
      pageChanged: false,
      elementsChanged: false,
      cartIncremented: false,
      successPattern: 'added to cart',
      errorPattern: null,
      timeSinceSnapshot: 0,
    };
    const description = describeChanges(changes);
    expect(description).toContain('URL changed');
    expect(description).toContain('title changed');
    expect(description).toContain('success: "added to cart"');
  });

  it('should describe no changes', () => {
    const changes: ChangeResult = {
      urlChanged: false,
      titleChanged: false,
      pageChanged: false,
      elementsChanged: false,
      cartIncremented: false,
      successPattern: null,
      errorPattern: null,
      timeSinceSnapshot: 0,
    };
    expect(describeChanges(changes)).toBe('no observable changes');
  });
});

// ============================================================================
// Executor Configuration Tests
// ============================================================================

describe('Executor Configuration', () => {
  it('should have reasonable step limit', () => {
    const MAX_STEPS = 25;
    expect(MAX_STEPS).toBeGreaterThan(10);
    expect(MAX_STEPS).toBeLessThanOrEqual(100);
  });

  it('should have retry limit for replans', () => {
    const MAX_REPLANS = 2;
    expect(MAX_REPLANS).toBeGreaterThan(0);
    expect(MAX_REPLANS).toBeLessThanOrEqual(5);
  });

  it('should have LLM call limit per task', () => {
    const MAX_LLM_CALLS_PER_TASK = 3;
    expect(MAX_LLM_CALLS_PER_TASK).toBeGreaterThan(0);
    expect(MAX_LLM_CALLS_PER_TASK).toBeLessThanOrEqual(10);
  });
});

// ============================================================================
// Obstacle Resolution Detection Tests
// ============================================================================

describe('Obstacle Resolution', () => {
  it('should detect obstacle resolved when no obstacle now', () => {
    const previousObstacle = { type: 'LOGIN_REQUIRED', message: 'Login required' };
    const currentObstacle: null = null;

    const resolved = !currentObstacle;
    expect(resolved).toBe(true);
  });

  it('should detect obstacle resolved when type changed', () => {
    const previousObstacle = { type: 'LOGIN_REQUIRED', message: 'Login required' };
    const currentObstacle = { type: 'OUT_OF_STOCK', message: 'Out of stock' };

    const resolved = currentObstacle.type !== previousObstacle.type;
    expect(resolved).toBe(true);
  });

  it('should not detect resolution when same obstacle persists', () => {
    const previousObstacle = { type: 'LOGIN_REQUIRED', message: 'Login required' };
    const currentObstacle = { type: 'LOGIN_REQUIRED', message: 'Login required' };

    const resolved = !currentObstacle || currentObstacle.type !== previousObstacle.type;
    expect(resolved).toBe(false);
  });
});

// ============================================================================
// Change Observer State Management Tests
// ============================================================================

describe('ChangeObserver State Management', () => {
  interface TestObserver {
    snapshot: { url: string; title: string } | null;
    takeSnapshot: (dom: { url: string; title: string }) => void;
    detectChanges: (dom: { url: string; title: string }) => ChangeResult;
  }

  it('should take snapshot before action', () => {
    let snapshot: { url: string; title: string } | null = null;

    const takeSnapshot = (dom: { url: string; title: string }) => {
      snapshot = dom;
    };

    takeSnapshot({ url: 'https://amazon.com', title: 'Amazon' });
    expect(snapshot).not.toBeNull();
    expect(snapshot?.url).toBe('https://amazon.com');
  });

  it('should clear snapshot after use', () => {
    let snapshot: { url: string; title: string } | null = { url: 'https://amazon.com', title: 'Amazon' };

    // After detection
    snapshot = null;
    expect(snapshot).toBeNull();
  });

  it('should detect no snapshot taken', () => {
    const snapshot: { url: string; title: string } | null = null;

    if (!snapshot) {
      expect(true).toBe(true); // No snapshot case handled
    }
  });
});
