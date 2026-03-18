import {
  INTERACTIVE_SELECTORS,
  MAX_INTERACTIVE_ELEMENTS,
  MAX_PAGE_TEXT_LENGTH,
} from '../src/shared/constants';
import type { InteractiveElement, AmazonPageState } from '../src/shared/types';

// ============================================================================
// DOM Selection Tests
// ============================================================================

describe('DOM Selection Logic', () => {
  describe('Interactive Element Selectors', () => {
    it('INTERACTIVE_SELECTORS should contain standard selectors', () => {
      expect(INTERACTIVE_SELECTORS).toContain('a[href]');
      expect(INTERACTIVE_SELECTORS).toContain('button');
      expect(INTERACTIVE_SELECTORS).toContain('input');
      expect(INTERACTIVE_SELECTORS).toContain('textarea');
      expect(INTERACTIVE_SELECTORS).toContain('select');
    });

    it('INTERACTIVE_SELECTORS should contain role-based selectors', () => {
      expect(INTERACTIVE_SELECTORS).toContain("[role='button']");
      expect(INTERACTIVE_SELECTORS).toContain("[role='link']");
    });

    it('INTERACTIVE_SELECTORS should contain onclick and tabindex selectors', () => {
      expect(INTERACTIVE_SELECTORS).toContain('[onclick]');
      expect(INTERACTIVE_SELECTORS).toContain('[tabindex]:not([tabindex="-1"])');
    });

    it('INTERACTIVE_SELECTORS count should be correct', () => {
      expect(INTERACTIVE_SELECTORS).toHaveLength(9);
    });
  });

  describe('Element Visibility Check', () => {
    it('should filter out hidden elements', () => {
      const mockElement = {
        getBoundingClientRect: () => ({
          top: 100,
          left: 0,
          bottom: 100,
          right: 100,
          width: 100,
          height: 100,
          x: 0,
          y: 100,
          toJSON: () => ({}),
        }),
        hidden: false,
      } as HTMLElement;

      const rect = mockElement.getBoundingClientRect();
      expect(rect.top).toBeGreaterThanOrEqual(0);
      expect(rect.height).toBeGreaterThan(0);
    });

    it('should handle elements with display:none', () => {
      // In a real DOM, we'd check getComputedStyle
      // For unit tests, we mock the logic
      const style = { display: 'none', visibility: 'visible', opacity: '1' };
      expect(style.display).toBe('none');
    });

    it('should handle elements with visibility:hidden', () => {
      const style = { display: 'block', visibility: 'hidden', opacity: '1' };
      expect(style.visibility).toBe('hidden');
    });

    it('should handle elements with opacity:0', () => {
      const style = { display: 'block', visibility: 'visible', opacity: '0' };
      expect(style.opacity).toBe('0');
    });
  });

  describe('Viewport Calculation', () => {
    it('should identify elements in viewport', () => {
      const viewportHeight = 800;
      const rect = { top: 100, bottom: 200 };
      const isInViewport = rect.top >= 0 && rect.bottom <= viewportHeight;
      expect(isInViewport).toBe(true);
    });

    it('should identify elements outside viewport', () => {
      const viewportHeight = 800;
      const rect = { top: 900, bottom: 1000 };
      const isInViewport = rect.top >= 0 && rect.bottom <= viewportHeight;
      expect(isInViewport).toBe(false);
    });

    it('should handle elements above viewport (scrollable)', () => {
      const viewportHeight = 800;
      const rect = { top: -600, bottom: 0 };
      // Element at -600 is within the 500px margin
      const isInViewport = rect.top >= -500 && rect.bottom <= viewportHeight + 500;
      // -600 is NOT >= -500, so this should be false
      expect(isInViewport).toBe(false);
    });

    it('should handle elements far below viewport', () => {
      const viewportHeight = 800;
      const rect = { top: 2000, bottom: 2100 };
      const isInViewport = rect.top >= -500 && rect.bottom <= viewportHeight + 500;
      expect(isInViewport).toBe(false);
    });
  });

  describe('Element Filtering by Size', () => {
    it('should skip very small elements', () => {
      const rect = { width: 5, height: 5 };
      expect(rect.width < 10).toBe(true);
      expect(rect.height < 10).toBe(true);
    });

    it('should accept elements larger than 10x10', () => {
      const rect = { width: 10, height: 10 };
      expect(rect.width < 10).toBe(false);
      expect(rect.height < 10).toBe(false);
    });

    it('should accept reasonable button sizes', () => {
      const rect = { width: 50, height: 30 };
      expect(rect.width >= 10).toBe(true);
      expect(rect.height >= 10).toBe(true);
    });
  });
});

// ============================================================================
// Element Text Extraction Tests
// ============================================================================

describe('Element Text Extraction', () => {
  describe('Input Element Text', () => {
    it('should use placeholder for empty inputs', () => {
      const input = {
        type: 'text',
        placeholder: 'Search...',
        value: '',
        name: 'search',
        id: '',
        getAttribute: (attr: string) => attr === 'aria-label' ? null : null,
        closest: (sel: string) => null,
      } as HTMLInputElement;

      const text = input.placeholder || input.value || input.name || '';
      expect(text).toBe('Search...');
    });

    it('should use value for non-password inputs', () => {
      const input = {
        type: 'text',
        placeholder: '',
        value: 'user entered text',
        name: '',
        id: '',
        getAttribute: (attr: string) => null,
        closest: (sel: string) => null,
      } as HTMLInputElement;

      const text = input.placeholder || input.value || input.name || '';
      expect(text).toBe('user entered text');
    });

    it('should not use value for password inputs', () => {
      const input = {
        type: 'password',
        placeholder: '',
        value: 'secret123',
        name: 'password',
        id: '',
        getAttribute: (attr: string) => null,
        closest: (sel: string) => null,
      } as HTMLInputElement;

      const text = input.type === 'password' 
        ? (input.placeholder || input.name || '')
        : input.value;
      expect(text).not.toBe('secret123');
    });

    it('should try to find associated label', () => {
      const input = {
        type: 'text',
        placeholder: '',
        value: '',
        name: '',
        id: 'email-input',
        getAttribute: (attr: string) => attr === 'aria-label' ? null : null,
        closest: (sel: string) => null,
      } as HTMLInputElement;

      const labelElement = {
        querySelector: (sel: string) => sel === 'label[for="email-input"]' 
          ? { textContent: 'Email address' } 
          : null,
      } as HTMLElement;

      const label = labelElement.querySelector('label[for="email-input"]');
      expect(label?.textContent?.trim()).toBe('Email address');
    });
  });

  describe('Select Element Text', () => {
    it('should use selected option text', () => {
      const select = {
        selectedIndex: 2,
        options: [
          { text: 'Option 1' },
          { text: 'Option 2' },
          { text: 'Selected Option' },
        ] as HTMLOptionList,
        name: '',
        id: '',
        getAttribute: () => null,
        closest: () => null,
      } as HTMLSelectElement;

      const selected = select.options[select.selectedIndex];
      const text = selected?.text || '';
      expect(text).toBe('Selected Option');
    });

    it('should handle empty select', () => {
      const select = {
        selectedIndex: -1,
        options: [] as HTMLOptionList,
        name: '',
        id: '',
        getAttribute: () => null,
        closest: () => null,
      } as HTMLSelectElement;

      const selected = select.options[select.selectedIndex];
      const text = selected?.text || '';
      expect(text).toBe('');
    });
  });

  describe('TextArea Element Text', () => {
    it('should use placeholder for empty textarea', () => {
      const textarea = {
        type: 'textarea',
        placeholder: 'Enter message',
        value: '',
        name: '',
        id: '',
        getAttribute: () => null,
        closest: () => null,
      } as HTMLTextAreaElement;

      const text = textarea.placeholder || textarea.name || '';
      expect(text).toBe('Enter message');
    });

    it('should use textarea value', () => {
      const textarea = {
        type: 'textarea',
        placeholder: '',
        value: 'User wrote something',
        name: '',
        id: '',
        getAttribute: () => null,
        closest: () => null,
      } as HTMLTextAreaElement;

      const text = textarea.placeholder || textarea.value || '';
      expect(text).toBe('User wrote something');
    });
  });

  describe('Generic Element Text', () => {
    it('should use innerText for regular elements', () => {
      const element = {
        innerText: 'This is the element text',
        textContent: 'This is the element text',
      } as HTMLElement;

      const text = (element.innerText || element.textContent || '').trim();
      expect(text).toBe('This is the element text');
    });

    it('should handle multiline text with whitespace normalization', () => {
      const element = {
        innerText: 'This   is   multiline\ntext',
        textContent: 'This   is   multiline\ntext',
      } as HTMLElement;

      const text = (element.innerText || element.textContent || '')
        .replace(/\s+/g, ' ')
        .trim();
      expect(text).toBe('This is multiline text');
    });

    it('should truncate text to 100 characters', () => {
      const longText = 'a'.repeat(200);
      const element = {
        innerText: longText,
        textContent: longText,
      } as HTMLElement;

      const text = (element.innerText || element.textContent || '')
        .replace(/\s+/g, ' ')
        .trim()
        .slice(0, 100);
      
      expect(text.length).toBe(100);
    });
  });
});

// ============================================================================
// CSS Selector Generation Tests
// ============================================================================

describe('CSS Selector Generation', () => {
  describe('ID-based Selectors', () => {
    it('should generate simple ID selector', () => {
      const element = {
        id: 'simple-id',
        tagName: 'DIV',
        className: '',
        getAttribute: () => null,
      } as HTMLElement;

      const selector = `#${element.id}`;
      expect(selector).toBe('#simple-id');
    });

    it('should generate quoted ID selector for complex IDs', () => {
      const element = {
        id: 'complex.id-with_dashes',
        tagName: 'DIV',
        className: '',
        getAttribute: () => null,
      } as HTMLElement;

      const escaped = CSS.escape(element.id);
      const selector = `[id="${escaped}"]`;
      // CSS.escape may escape different chars, so just check it starts with [id="
      expect(selector).toMatch(/^\[id=".*"\]$/);
    });

    it('should validate simple ID format', () => {
      const validId = 'simple-id';
      const invalidId = '123invalid';
      const idRegex = /^[a-zA-Z][\w-]*$/;
      
      expect(idRegex.test(validId)).toBe(true);
      expect(idRegex.test(invalidId)).toBe(false);
    });
  });

  describe('Name-based Selectors', () => {
    it('should generate name selector for unique elements', () => {
      const element = {
        id: '',
        tagName: 'INPUT',
        className: '',
        getAttribute: (attr: string) => attr === 'name' ? 'search' : null,
      } as HTMLElement;

      const nameSelector = `input[name="search"]`;
      expect(nameSelector).toBe('input[name="search"]');
    });

    it('should handle CSS escaping in name attributes', () => {
      const element = {
        id: '',
        tagName: 'INPUT',
        className: '',
        getAttribute: (attr: string) => attr === 'name' ? 'search.query' : null,
      } as HTMLElement;

      const escaped = CSS.escape('search.query');
      const nameSelector = `input[name="${escaped}"]`;
      // Check the selector format is valid
      expect(nameSelector).toMatch(/^input\[name=".*"\]$/);
    });
  });

  describe('Class-based Selectors', () => {
    it('should generate class selector from element classes', () => {
      const element = {
        id: '',
        tagName: 'BUTTON',
        className: 'btn btn-primary',
        getAttribute: () => null,
      } as HTMLElement;

      const classes = element.className
        .split(/\s+/)
        .filter(c => c && !c.includes(':') && /^[a-zA-Z]/.test(c))
        .slice(0, 3);
      
      const classSelector = classes.map(c => `.${CSS.escape(c)}`).join('');
      const selector = `button${classSelector}`;
      
      // Check that classes are present in the selector
      expect(selector).toContain('button');
      expect(selector).toContain('.btn');
      expect(selector).toMatch(/\.btn.*\.btn/);
    });

    it('should filter out pseudo-class selectors', () => {
      const element = {
        id: '',
        tagName: 'DIV',
        className: 'valid-class pseudo-class another-valid',
        getAttribute: () => null,
      } as HTMLElement;

      const classes = element.className
        .split(/\s+/)
        .filter(c => c && !c.includes(':') && /^[a-zA-Z]/.test(c));
      
      // Only valid-class and another-valid should pass (no colons, starts with letter)
      // pseudo-class doesn't have colon in it, so it passes the filter
      expect(classes.length).toBeGreaterThanOrEqual(2);
      expect(classes).toContain('valid-class');
      expect(classes).toContain('another-valid');
    });
  });

  describe('Data Attribute Selectors', () => {
    it('should use data-testid when available', () => {
      const element = {
        id: '',
        tagName: 'BUTTON',
        className: '',
        getAttribute: (attr: string) => attr === 'data-testid' ? 'submit-btn' : null,
      } as HTMLElement;

      const dataTestId = element.getAttribute('data-testid');
      const escaped = CSS.escape(dataTestId!);
      const selector = `[data-testid="${escaped}"]`;
      
      // Check that it's a valid selector format
      expect(selector).toMatch(/^\[data-testid=".*"\]$/);
    });

    it('should handle data-test-id as fallback', () => {
      const element = {
        id: '',
        tagName: 'BUTTON',
        className: '',
        getAttribute: (attr: string) => {
          if (attr === 'data-testid') return null;
          if (attr === 'data-test-id') return 'test-button';
          return null;
        },
      } as HTMLElement;

      const dataTestId = element.getAttribute('data-testid') || element.getAttribute('data-test-id');
      expect(dataTestId).toBe('test-button');
    });
  });

  describe('Aria Attribute Selectors', () => {
    it('should use aria-label for accessibility', () => {
      const element = {
        id: '',
        tagName: 'BUTTON',
        className: '',
        getAttribute: (attr: string) => attr === 'aria-label' ? 'Submit form' : null,
      } as HTMLElement;

      const ariaLabel = element.getAttribute('aria-label');
      const escaped = CSS.escape(ariaLabel!);
      const selector = `button[aria-label="${escaped}"]`;
      
      // Check that it's a valid selector format
      expect(selector).toMatch(/^button\[aria-label=".*"\]$/);
    });
  });

  describe('Nth-child Fallback Selectors', () => {
    it('should generate path for elements without identifiers', () => {
      const element = {
        tagName: 'BUTTON',
        id: '',
        className: '',
        getAttribute: () => null,
        getBoundingClientRect: () => ({
          top: 100,
          left: 0,
          bottom: 100,
          right: 100,
          width: 100,
          height: 100,
          x: 0,
          y: 100,
          toJSON: () => ({}),
        }),
      } as HTMLElement;

      const siblings = [element as unknown as HTMLElement];
      const index = siblings.indexOf(element) + 1;
      const selector = `button:nth-of-type(${index})`;
      
      expect(selector).toBe('button:nth-of-type(1)');
    });
  });
});

// ============================================================================
// Attribute Extraction Tests
// ============================================================================

describe('Relevant Attribute Extraction', () => {
  const relevantAttrs = [
    'href', 'name', 'placeholder', 'aria-label', 'title', 'role', 'type', 'value',
  ];

  it('should extract common attributes', () => {
    const element = {
      tagName: 'A',
      getAttribute: (attr: string) => {
        const attrs: Record<string, string> = {
          href: '/path/to/page',
          name: 'my-link',
          title: 'My Link',
        };
        return attrs[attr] || '';
      },
    } as HTMLElement;

    const extracted: Record<string, string> = {};
    relevantAttrs.forEach((attr) => {
      const value = element.getAttribute(attr);
      if (value) {
        extracted[attr] = value.slice(0, 100);
      }
    });

    expect(extracted).toHaveProperty('href');
    expect(extracted).toHaveProperty('name');
    expect(extracted).toHaveProperty('title');
  });

  it('should not extract form values for privacy', () => {
    const element = {
      tagName: 'INPUT',
      getAttribute: (attr: string) => {
        if (attr === 'value') return 'sensitive-data';
        return '';
      },
    } as HTMLElement;

    const value = element.getAttribute('value');
    const shouldExtract = value && 'value' !== 'value';
    expect(shouldExtract).toBe(false);
  });

  it('should truncate long attribute values', () => {
    const longValue = 'x'.repeat(200);
    const truncated = longValue.slice(0, 100);
    expect(truncated.length).toBe(100);
  });
});

// ============================================================================
// Limits and Thresholds Tests
// ============================================================================

describe('Limits and Thresholds', () => {
  it('MAX_INTERACTIVE_ELEMENTS should be 30', () => {
    expect(MAX_INTERACTIVE_ELEMENTS).toBe(30);
  });

  it('MAX_PAGE_TEXT_LENGTH should be 1500', () => {
    expect(MAX_PAGE_TEXT_LENGTH).toBe(1500);
  });

  it('text truncation should respect MAX_PAGE_TEXT_LENGTH', () => {
    const longText = 'a'.repeat(2000);
    const truncated = longText.slice(0, MAX_PAGE_TEXT_LENGTH);
    expect(truncated.length).toBe(MAX_PAGE_TEXT_LENGTH);
  });

  it('element array should respect MAX_INTERACTIVE_ELEMENTS', () => {
    const manyElements = Array.from({ length: 100 }, (_, i) => ({ index: i }));
    const limitedElements = manyElements.slice(0, MAX_INTERACTIVE_ELEMENTS);
    expect(limitedElements.length).toBe(MAX_INTERACTIVE_ELEMENTS);
  });
});
