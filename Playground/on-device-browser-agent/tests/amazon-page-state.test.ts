import {
  AMAZON_SELECTORS,
  AMAZON_URL_PATTERNS,
  AMAZON_SUCCESS_PATTERNS,
  AMAZON_OBSTACLE_PATTERNS,
} from '../src/shared/constants';

describe('Amazon Page State Detection', () => {
  describe('URL Pattern Matching', () => {
    describe('Homepage Detection', () => {
      const homepageTests = [
        { url: 'https://www.amazon.com/', expected: true },
        { url: 'https://amazon.com/', expected: true },
        { url: 'http://www.amazon.co.uk/', expected: true },
        { url: 'https://www.amazon.com/de/', expected: false }, // Locale subpath, not homepage
        { url: 'https://www.google.com/', expected: false },
        { url: 'https://amazon.com/some/path', expected: false },
      ];

      homepageTests.forEach(({ url, expected }) => {
        it(`should ${expected ? 'match' : 'not match'}: ${url}`, () => {
          const result = AMAZON_URL_PATTERNS.homepage.test(url);
          expect(result).toBe(expected);
        });
      });
    });

    describe('Search Results Detection', () => {
      const searchTests = [
        { url: 'https://www.amazon.com/s?k=iphone', expected: true },
        { url: 'https://amazon.com/s?k=test', expected: true },
        { url: 'https://www.amazon.com/dp/B08EXAMPLE', expected: false },
        { url: 'https://www.amazon.com/', expected: false },
      ];

      searchTests.forEach(({ url, expected }) => {
        it(`should ${expected ? 'match' : 'not match'}: ${url}`, () => {
          const result = AMAZON_URL_PATTERNS.search.test(url);
          expect(result).toBe(expected);
        });
      });
    });

    describe('Product Page Detection', () => {
      const productTests = [
        { url: 'https://www.amazon.com/dp/B08EXAMPLE', expected: true },
        { url: 'https://www.amazon.com/gp/product/123', expected: true },
        { url: 'https://www.amazon.com/gp/buy', expected: false },
        { url: 'https://www.amazon.com/s?k=test', expected: false },
      ];

      productTests.forEach(({ url, expected }) => {
        it(`should ${expected ? 'match' : 'not match'}: ${url}`, () => {
          const result = AMAZON_URL_PATTERNS.product.test(url);
          expect(result).toBe(expected);
        });
      });
    });

    describe('Cart Detection', () => {
      const cartTests = [
        { url: 'https://www.amazon.com/gp/cart/view.html', expected: true },
        { url: 'https://amazon.com/gp/cart', expected: true },
        { url: 'https://www.amazon.com/dp/B08EXAMPLE', expected: false },
        { url: 'https://www.amazon.com/checkout', expected: false },
      ];

      cartTests.forEach(({ url, expected }) => {
        it(`should ${expected ? 'match' : 'not match'}: ${url}`, () => {
          const result = AMAZON_URL_PATTERNS.cart.test(url);
          expect(result).toBe(expected);
        });
      });
    });

    describe('Sign In Detection', () => {
      const signinTests = [
        { url: 'https://www.amazon.com/ap/signin', expected: true },
        { url: 'https://www.amazon.com/ap/signin?whatever', expected: true },
        { url: 'https://www.amazon.com/', expected: false },
        { url: 'https://www.amazon.com/dp/B08EXAMPLE', expected: false },
      ];

      signinTests.forEach(({ url, expected }) => () => {
        const result = AMAZON_URL_PATTERNS.signin.test(url);
        expect(result).toBe(expected);
      });
    });

    describe('Checkout Detection', () => {
      const checkoutTests = [
        { url: 'https://www.amazon.com/gp/buy', expected: true },
        { url: 'https://amazon.com/checkout', expected: true },
        { url: 'https://www.amazon.com/cart', expected: false },
        { url: 'https://www.amazon.com/dp/B08EXAMPLE', expected: false },
      ];

      checkoutTests.forEach(({ url, expected }) => {
        it(`should ${expected ? 'match' : 'not match'}: ${url}`, () => {
          const result = AMAZON_URL_PATTERNS.checkout.test(url);
          expect(result).toBe(expected);
        });
      });
    });
  });

  describe('CSS Selectors', () => {
    it('should have search input selector', () => {
      expect(AMAZON_SELECTORS.searchInput).toBe('#twotabsearchtextbox');
    });

    it('should have search button selector', () => {
      expect(AMAZON_SELECTORS.searchButton).toBe('#nav-search-submit-button');
    });

    it('should have add to cart button selector', () => {
      expect(AMAZON_SELECTORS.addToCartButton).toBe('#add-to-cart-button');
    });

    it('should have buy now button selector', () => {
      expect(AMAZON_SELECTORS.buyNowButton).toBe('#buy-now-button');
    });

    it('should have cart count selector', () => {
      expect(AMAZON_SELECTORS.cartCount).toBe('#nav-cart-count');
    });

    it('should have captcha form selector', () => {
      expect(AMAZON_SELECTORS.captchaForm).toBe('form[action*="captcha"]');
    });

    it('should have signin form selector', () => {
      expect(AMAZON_SELECTORS.signinForm).toBe('form[name="signIn"]');
    });

    it('should have product card selector', () => {
      expect(AMAZON_SELECTORS.productCard).toBe('[data-component-type="s-search-result"]');
    });

    it('should have product title selector', () => {
      expect(AMAZON_SELECTORS.productTitle).toBe('h2 a.a-link-normal');
    });

    it('should have product price selector', () => {
      expect(AMAZON_SELECTORS.productPrice).toBe('.a-price .a-offscreen');
    });

    it('should have see all buying options selector', () => {
      expect(AMAZON_SELECTORS.seeAllBuyingOptions).toBe('#buybox-see-all-buying-choices');
    });

    it('should have add to cart confirm selector', () => {
      expect(AMAZON_SELECTORS.addedToCartConfirm).toBe('#NATC_SMART_WAGON_CONF_MSG_SUCCESS');
    });

    it('should have side cart view cart selector', () => {
      expect(AMAZON_SELECTORS.sideCartViewCart).toBe('#attach-sidesheet-view-cart-button');
    });

    it('should have out of stock selector', () => {
      expect(AMAZON_SELECTORS.outOfStock).toBe('#outOfStock');
    });

    it('should have product title main selector', () => {
      expect(AMAZON_SELECTORS.productTitleMain).toBe('#productTitle');
    });

    it('should have sponsored label selector', () => {
      expect(AMAZON_SELECTORS.sponsoredLabel).toBe('.s-label-popover-default');
    });
  });

  describe('Success Pattern Detection', () => {
    it('should have "addedToCart" patterns', () => {
      const patterns = AMAZON_SUCCESS_PATTERNS.addedToCart;
      expect(patterns).toContain('added to cart');
      expect(patterns).toContain('added to your cart');
      expect(patterns).toContain('1 item added to cart');
    });

    it('should have "searchResults" patterns', () => {
      const patterns = AMAZON_SUCCESS_PATTERNS.searchResults;
      expect(patterns).toContain('results for');
      expect(patterns).toContain('over');
      expect(patterns).toContain('of results');
    });

    it('should have "productPage" patterns', () => {
      const patterns = AMAZON_SUCCESS_PATTERNS.productPage;
      expect(patterns).toContain('add to cart');
      expect(patterns).toContain('buy now');
    });
  });

  describe('Obstacle Pattern Detection', () => {
    it('should have "login" patterns', () => {
      const patterns = AMAZON_OBSTACLE_PATTERNS.login;
      expect(patterns).toContain('sign in');
      expect(patterns).toContain('sign-in');
      expect(patterns).toContain('create account');
    });

    it('should have "captcha" patterns', () => {
      const patterns = AMAZON_OBSTACLE_PATTERNS.captcha;
      expect(patterns).toContain('enter the characters');
      expect(patterns).toContain('type the characters');
      expect(patterns).toContain('robot');
    });

    it('should have "outOfStock" patterns', () => {
      const patterns = AMAZON_OBSTACLE_PATTERNS.outOfStock;
      expect(patterns).toContain('currently unavailable');
      expect(patterns).toContain('out of stock');
      expect(patterns).toContain('not available');
    });

    it('should have "priceChange" patterns', () => {
      const patterns = AMAZON_OBSTACLE_PATTERNS.priceChange;
      expect(patterns).toContain('price changed');
      expect(patterns).toContain('price has changed');
    });
  });

  describe('Pattern Matching Helpers', () => {
    describe('isAmazonPageUrl', () => {
      it('should detect Amazon URLs', () => {
        expect(AMAZON_URL_PATTERNS.homepage.test('https://www.amazon.com/')).toBe(true);
        expect(AMAZON_URL_PATTERNS.search.test('https://www.amazon.com/s?k=test')).toBe(true);
        expect(AMAZON_URL_PATTERNS.product.test('https://www.amazon.com/dp/B08TEST')).toBe(true);
        expect(AMAZON_URL_PATTERNS.cart.test('https://www.amazon.com/gp/cart')).toBe(true);
      });

      it('should reject non-Amazon URLs', () => {
        expect(AMAZON_URL_PATTERNS.homepage.test('https://www.google.com/')).toBe(false);
        expect(AMAZON_URL_PATTERNS.homepage.test('https://www.ebay.com/')).toBe(false);
        expect(AMAZON_URL_PATTERNS.homepage.test('https://amazon.com/de/')).toBe(false);
      });
    });
  });
});
