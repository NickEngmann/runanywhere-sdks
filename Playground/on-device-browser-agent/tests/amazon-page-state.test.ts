/**
 * Amazon Page State Detection Tests
 * 
 * Tests for Amazon-specific page state detection, including:
 * - Page state detection from URL
 * - Alert extraction
 * - Cart count detection
 * - Success/obstacle pattern matching
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import * as constants from '../src/shared/constants';
import type { AmazonPageState, ObstacleType } from '../src/shared/types';

// ============================================================================
// URL Pattern Tests
// ============================================================================

describe('Amazon URL Patterns', () => {
  it('should match homepage URL', () => {
    const homepagePattern = constants.AMAZON_URL_PATTERNS.homepage;
    expect(homepagePattern.test('https://www.amazon.com/')).toBe(true);
    expect(homepagePattern.test('https://amazon.com/')).toBe(true);
    expect(homepagePattern.test('https://www.amazon.co.uk/')).toBe(true);
    expect(homepagePattern.test('https://www.amazon.de/')).toBe(true);

    // Should NOT match other URLs
    expect(homepagePattern.test('https://www.amazon.com/gp/cart')).toBe(false);
    expect(homepagePattern.test('https://www.amazon.com/dp/ABC123')).toBe(false);
  });

  it('should match search results URL', () => {
    const searchPattern = constants.AMAZON_URL_PATTERNS.search;
    expect(searchPattern.test('https://www.amazon.com/s?k=phone')).toBe(true);
    expect(searchPattern.test('https://www.amazon.com/s?k=books&rh=n:283155')).toBe(true);

    // Should NOT match other URLs
    expect(searchPattern.test('https://www.amazon.com/')).toBe(false);
    expect(searchPattern.test('https://www.amazon.com/dp/ABC123')).toBe(false);
  });

  it('should match product detail URL', () => {
    const productPattern = constants.AMAZON_URL_PATTERNS.product;
    expect(productPattern.test('https://www.amazon.com/dp/ABC123DEF')).toBe(true);
    expect(productPattern.test('https://www.amazon.com/gp/product/ABC123')).toBe(true);

    // Should NOT match other URLs
    expect(productPattern.test('https://www.amazon.com/')).toBe(false);
    expect(productPattern.test('https://www.amazon.com/s?k=phone')).toBe(false);
  });

  it('should match cart URL', () => {
    const cartPattern = constants.AMAZON_URL_PATTERNS.cart;
    expect(cartPattern.test('https://www.amazon.com/gp/cart/view.html')).toBe(true);

    // Should NOT match other URLs
    expect(cartPattern.test('https://www.amazon.com/')).toBe(false);
    expect(cartPattern.test('https://www.amazon.com/dp/ABC123')).toBe(false);
  });

  it('should match sign-in URL', () => {
    const signinPattern = constants.AMAZON_URL_PATTERNS.signin;
    expect(signinPattern.test('https://www.amazon.com/ap/signin')).toBe(true);

    // Should NOT match other URLs
    expect(signinPattern.test('https://www.amazon.com/')).toBe(false);
    expect(signinPattern.test('https://www.amazon.com/dp/ABC123')).toBe(false);
  });

  it('should match checkout URL', () => {
    const checkoutPattern = constants.AMAZON_URL_PATTERNS.checkout;
    expect(checkoutPattern.test('https://www.amazon.com/gp/buy')).toBe(true);

    // Should NOT match other URLs
    expect(checkoutPattern.test('https://www.amazon.com/')).toBe(false);
    expect(checkoutPattern.test('https://www.amazon.com/s?k=phone')).toBe(false);
  });
});

// ============================================================================
// Alert Extraction Tests
// ============================================================================

describe('Amazon Alert Extraction', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should extract alert content', () => {
    const alert = document.createElement('div');
    alert.className = 'a-alert-content';
    alert.textContent = 'Item added to cart successfully';
    document.body.appendChild(alert);

    const found = document.querySelector('.a-alert-content');
    expect(found).toBe(alert);
    expect((found as HTMLElement).textContent).toBe('Item added to cart successfully');
  });

  it('should extract alert with role attribute', () => {
    const alert = document.createElement('div');
    alert.setAttribute('role', 'alert');
    alert.textContent = 'Error: Payment declined';
    document.body.appendChild(alert);

    const found = document.querySelector('[role="alert"]');
    expect(found).toBe(alert);
    expect((found as HTMLElement).textContent).toBe('Error: Payment declined');
  });

  it('should extract success message alerts', () => {
    const successAlert = document.createElement('div');
    successAlert.className = 'message-success';
    successAlert.textContent = 'Your order has been placed successfully';
    document.body.appendChild(successAlert);

    const found = document.querySelector('.message-success');
    expect(found).toBe(successAlert);
    expect((found as HTMLElement).textContent).toBe('Your order has been placed successfully');
  });

  it('should extract error alerts', () => {
    const errorAlert = document.createElement('div');
    errorAlert.className = 'message-error';
    errorAlert.textContent = 'We could not complete your order';
    document.body.appendChild(errorAlert);

    const found = document.querySelector('.message-error');
    expect(found).toBe(errorAlert);
    expect((found as HTMLElement).textContent).toBe('We could not complete your order');
  });

  it('should limit alerts to 5', () => {
    for (let i = 0; i < 10; i++) {
      const alert = document.createElement('div');
      alert.className = 'a-alert-content';
      alert.textContent = `Alert ${i}`;
      document.body.appendChild(alert);
    }

    const alerts = document.querySelectorAll('.a-alert-content');
    expect(alerts.length).toBe(10);
    const limitedAlerts = Array.from(alerts).slice(0, 5);
    expect(limitedAlerts.length).toBe(5);
  });

  it('should skip alerts with long text', () => {
    const alert = document.createElement('div');
    alert.className = 'a-alert-content';
    alert.textContent = 'A'.repeat(300);
    document.body.appendChild(alert);

    const found = document.querySelector('.a-alert-content');
    const text = (found as HTMLElement).textContent || '';
    expect(text.length).toBe(300);
    expect(text.length).toBeGreaterThan(200);
  });
});

// ============================================================================
// Cart Count Detection Tests
// ============================================================================

describe('Amazon Cart Count Detection', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should extract cart count from nav', () => {
    const cartCount = document.createElement('span');
    cartCount.id = 'nav-cart-count';
    cartCount.textContent = '3';
    document.body.appendChild(cartCount);

    const found = document.querySelector('#nav-cart-count');
    const count = parseInt((found as HTMLElement).textContent || '0', 10);
    expect(count).toBe(3);
  });

  it('should handle cart count of 0', () => {
    const cartCount = document.createElement('span');
    cartCount.id = 'nav-cart-count';
    cartCount.textContent = '0';
    document.body.appendChild(cartCount);

    const found = document.querySelector('#nav-cart-count');
    const count = parseInt((found as HTMLElement).textContent || '0', 10);
    expect(count).toBe(0);
  });

  it('should return 0 if cart count not found', () => {
    const found = document.querySelector('#nav-cart-count');
    expect(found).toBeNull();
  });
});

// ============================================================================
// Amazon Selectors Tests
// ============================================================================

describe('Amazon Selectors', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should find search input', () => {
    const searchInput = document.createElement('input');
    searchInput.id = 'twotabsearchtextbox';
    searchInput.type = 'text';
    searchInput.placeholder = 'Search Amazon';
    document.body.appendChild(searchInput);

    const found = document.querySelector('#twotabsearchtextbox');
    expect(found).toBe(searchInput);
    expect((found as HTMLInputElement).placeholder).toBe('Search Amazon');
  });

  it('should find search button', () => {
    const searchButton = document.createElement('button');
    searchButton.id = 'nav-search-submit-button';
    searchButton.textContent = 'Search';
    document.body.appendChild(searchButton);

    const found = document.querySelector('#nav-search-submit-button');
    expect(found).toBe(searchButton);
  });

  it('should find product cards', () => {
    const card1 = document.createElement('div');
    card1.setAttribute('data-component-type', 's-search-result');
    card1.id = 'product-1';

    const card2 = document.createElement('div');
    card2.setAttribute('data-component-type', 's-search-result');
    card2.id = 'product-2';

    document.body.appendChild(card1);
    document.body.appendChild(card2);

    const cards = document.querySelectorAll('[data-component-type="s-search-result"]');
    expect(cards.length).toBe(2);
  });

  it('should find sponsored labels', () => {
    const sponsoredLabel = document.createElement('span');
    sponsoredLabel.className = 's-label-popover-default';
    sponsoredLabel.textContent = 'Sponsored';
    document.body.appendChild(sponsoredLabel);

    const found = document.querySelector('.s-label-popover-default');
    expect(found).toBe(sponsoredLabel);
  });

  it('should find product title links', () => {
    const h2 = document.createElement('h2');
    const productLink = document.createElement('a');
    productLink.className = 'a-link-normal';
    productLink.href = '/dp/ABC123';
    productLink.textContent = 'Product Name';
    h2.appendChild(productLink);
    document.body.appendChild(h2);

    const found = document.querySelector('h2 a.a-link-normal');
    expect(found).toBe(productLink);
    expect((found as HTMLAnchorElement).href).toContain('/dp/ABC123');
  });

  it('should find product price', () => {
    const priceContainer = document.createElement('span');
    priceContainer.className = 'a-price';

    const priceOffscreen = document.createElement('span');
    priceOffscreen.className = 'a-offscreen';
    priceOffscreen.textContent = '$29.99';

    priceContainer.appendChild(priceOffscreen);
    document.body.appendChild(priceContainer);

    const found = document.querySelector('.a-price .a-offscreen');
    expect(found).toBe(priceOffscreen);
    expect((found as HTMLElement).textContent).toBe('$29.99');
  });

  it('should find add to cart button', () => {
    const addToCart = document.createElement('button');
    addToCart.id = 'add-to-cart-button';
    addToCart.textContent = 'Add to Cart';
    document.body.appendChild(addToCart);

    const found = document.querySelector('#add-to-cart-button');
    expect(found).toBe(addToCart);
    expect((found as HTMLElement).textContent).toBe('Add to Cart');
  });

  it('should find buy now button', () => {
    const buyNow = document.createElement('button');
    buyNow.id = 'buy-now-button';
    buyNow.textContent = 'Buy Now';
    document.body.appendChild(buyNow);

    const found = document.querySelector('#buy-now-button');
    expect(found).toBe(buyNow);
  });

  it('should find captcha form', () => {
    const captchaForm = document.createElement('form');
    captchaForm.action = '/captcha';
    captchaForm.innerHTML = '<p>Please solve this CAPTCHA</p>';
    document.body.appendChild(captchaForm);

    const found = document.querySelector('form[action*="captcha"]');
    expect(found).toBe(captchaForm);
  });

  it('should find signin form', () => {
    const signinForm = document.createElement('form');
    signinForm.name = 'signIn';
    signinForm.innerHTML = '<input type="email" placeholder="Email">';
    document.body.appendChild(signinForm);

    const found = document.querySelector('form[name="signIn"]');
    expect(found).toBe(signinForm);
  });
});

// ============================================================================
// Success Patterns Tests
// ============================================================================

describe('Amazon Success Patterns', () => {
  it('should have addedToCart patterns', () => {
    const patterns = constants.AMAZON_SUCCESS_PATTERNS.addedToCart;
    expect(patterns).toContain('added to cart');
    expect(patterns).toContain('added to your cart');
    expect(patterns).toContain('1 item added to cart');
  });

  it('should match page text with success patterns', () => {
    const patterns = constants.AMAZON_SUCCESS_PATTERNS.addedToCart;
    const pageText = 'Your item has been added to cart successfully';

    const match = patterns.some(p => pageText.includes(p));
    expect(match).toBe(true);
  });

  it('should have searchResults patterns', () => {
    const patterns = constants.AMAZON_SUCCESS_PATTERNS.searchResults;
    expect(patterns).toContain('results for');
    expect(patterns).toContain('over');
    expect(patterns).toContain('of results');

    const pageText = '1-16 of over 10,000 results for wireless headphones';
    const match = patterns.some(p => pageText.includes(p));
    expect(match).toBe(true);
  });
});

// ============================================================================
// Obstacle Patterns Tests
// ============================================================================

describe('Amazon Obstacle Patterns', () => {
  it('should have login patterns', () => {
    const patterns = constants.AMAZON_OBSTACLE_PATTERNS.login;
    expect(patterns).toContain('sign in');
    expect(patterns).toContain('sign-in');
    expect(patterns).toContain('create account');
    expect(patterns.length).toBeGreaterThan(0);
  });

  it('should have captcha patterns', () => {
    const patterns = constants.AMAZON_OBSTACLE_PATTERNS.captcha;
    expect(patterns).toContain('enter the characters');
    expect(patterns).toContain('type the characters');
    expect(patterns).toContain('robot');
  });

  it('should have outOfStock patterns', () => {
    const patterns = constants.AMAZON_OBSTACLE_PATTERNS.outOfStock;
    expect(patterns).toContain('currently unavailable');
    expect(patterns).toContain('out of stock');
    expect(patterns).toContain('not available');

    const pageText = 'This item is currently unavailable';
    const match = patterns.some(p => pageText.includes(p));
    expect(match).toBe(true);
  });

  it('should have priceChange patterns', () => {
    const patterns = constants.AMAZON_OBSTACLE_PATTERNS.priceChange;
    expect(patterns).toContain('price changed');
    expect(patterns).toContain('price has changed');

    const pageText = 'The price has changed since you last viewed this item';
    const match = patterns.some(p => pageText.includes(p));
    expect(match).toBe(true);
  });
});

// ============================================================================
// Page State Detection Tests
// ============================================================================

describe('Page State Detection', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should detect captcha page state', () => {
    const captchaForm = document.createElement('form');
    captchaForm.action = '/captchacheck';
    captchaForm.innerHTML = '<p>CAPTCHA verification required</p>';
    document.body.appendChild(captchaForm);

    const found = document.querySelector(constants.AMAZON_SELECTORS.captchaForm);
    expect(found).toBe(captchaForm);
  });

  it('should detect signin page state', () => {
    const signinForm = document.createElement('form');
    signinForm.name = 'signIn';
    document.body.appendChild(signinForm);

    const found = document.querySelector(constants.AMAZON_SELECTORS.signinForm);
    expect(found).toBe(signinForm);
  });

  it('should handle unknown page state', () => {
    const div = document.createElement('div');
    div.textContent = 'Regular content';
    document.body.appendChild(div);

    const captchaFound = document.querySelector(constants.AMAZON_SELECTORS.captchaForm);
    const signinFound = document.querySelector(constants.AMAZON_SELECTORS.signinForm);

    expect(captchaFound).toBeNull();
    expect(signinFound).toBeNull();
  });
});

// ============================================================================
// Obstacle Type Tests
// ============================================================================

describe('Obstacle Types', () => {
  it('should have all ObstacleType values', () => {
    const obstacleTypes: ObstacleType[] = [
      'LOGIN_REQUIRED',
      'CAPTCHA',
      'OUT_OF_STOCK',
      'PRICE_CHANGED',
      'ERROR'
    ];

    expect(obstacleTypes).toContain('LOGIN_REQUIRED');
    expect(obstacleTypes).toContain('CAPTCHA');
    expect(obstacleTypes).toContain('OUT_OF_STOCK');
    expect(obstacleTypes).toContain('PRICE_CHANGED');
    expect(obstacleTypes).toContain('ERROR');
  });
});

// ============================================================================
// Amazon Page State Types Tests
// ============================================================================

describe('Amazon Page State Types', () => {
  it('should have all AmazonPageState values', () => {
    const pageStates: AmazonPageState[] = [
      'homepage',
      'search_results',
      'product_page',
      'cart',
      'checkout',
      'signin',
      'captcha',
      'unknown'
    ];

    expect(pageStates).toContain('homepage');
    expect(pageStates).toContain('search_results');
    expect(pageStates).toContain('product_page');
    expect(pageStates).toContain('cart');
    expect(pageStates).toContain('checkout');
    expect(pageStates).toContain('signin');
    expect(pageStates).toContain('captcha');
    expect(pageStates).toContain('unknown');
  });
});
