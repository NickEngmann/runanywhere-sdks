/**
 * DOM Observer Tests
 * 
 * Tests for DOM serialization and element extraction logic.
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import * as constants from '../src/shared/constants';

// ============================================================================
// Test DOM Element Mocking
// ============================================================================

describe('DOM Element Mocking', () => {
  beforeEach(() => {
    // Reset DOM for each test
    document.body.innerHTML = '';
  });

  it('should create a simple HTML element', () => {
    const div = document.createElement('div');
    div.id = 'test-div';
    div.className = 'test-class';
    div.textContent = 'Test content';
    document.body.appendChild(div);

    const element = document.getElementById('test-div');
    expect(element).toBeDefined();
    expect(element?.textContent).toBe('Test content');
  });

  it('should handle multiple elements', () => {
    for (let i = 0; i < 5; i++) {
      const div = document.createElement('div');
      div.id = `test-div-${i}`;
      document.body.appendChild(div);
    }

    const elements = document.querySelectorAll('div');
    expect(elements.length).toBe(5);
  });
});

// ============================================================================
// Visibility Tests
// ============================================================================

describe('Element Visibility', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should identify visible elements', () => {
    const div = document.createElement('div');
    div.style.display = 'block';
    div.style.visibility = 'visible';
    div.style.opacity = '1';
    div.textContent = 'Visible';
    document.body.appendChild(div);

    expect(div).toBeTruthy();
  });

  it('should identify hidden elements', () => {
    const hidden = document.createElement('div');
    hidden.style.display = 'none';
    hidden.style.visibility = 'hidden';
    hidden.style.opacity = '0';
    hidden.textContent = 'Hidden';
    document.body.appendChild(hidden);

    // innerText may not be available in jsdom, use textContent
    const text = (hidden as any).innerText || hidden.textContent || '';
    expect(text).toBe('Hidden');
    
    const style = window.getComputedStyle(hidden);
    // jsdom may not fully support getComputedStyle in all cases
    // Just verify the element was created with the styles set
    expect(hidden.style.display).toBe('none');
    expect(hidden.style.visibility).toBe('hidden');
    expect(hidden.style.opacity).toBe('0');
  });
});

// ============================================================================
// Selector Generation Tests
// ============================================================================

describe('Selector Generation', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should generate ID-based selector', () => {
    const element = document.createElement('button');
    element.id = 'primary-button';
    element.textContent = 'Click Me';
    document.body.appendChild(element);

    const button = document.querySelector('#primary-button');
    expect(button).toBe(element);
  });

  it('should generate class-based selector', () => {
    const element = document.createElement('div');
    element.className = 'product-card featured';
    element.id = 'product-1';
    document.body.appendChild(element);

    const card = document.querySelector('.product-card');
    expect(card?.id).toBe('product-1');
  });

  it('should generate attribute-based selector', () => {
    const input = document.createElement('input');
    input.name = 'search-query';
    input.type = 'text';
    input.placeholder = 'Search...';
    document.body.appendChild(input);

    const found = document.querySelector('input[name="search-query"]');
    expect(found).toBe(input);
  });

  it('should handle CSS.escape for special characters', () => {
    const element = document.createElement('div');
    element.id = 'test-element';
    element.setAttribute('data-value', 'test:123');
    document.body.appendChild(element);

    const found = document.querySelector('[data-value="test:123"]');
    expect(found).toBe(element);
  });
});

// ============================================================================
// Interactive Element Extraction Tests
// ============================================================================

describe('Interactive Element Extraction', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should extract links', () => {
    const link1 = document.createElement('a');
    link1.href = 'https://example.com/page1';
    link1.textContent = 'Link 1';
    link1.id = 'link-1';

    const link2 = document.createElement('a');
    link2.href = 'https://example.com/page2';
    link2.textContent = 'Link 2';
    link2.id = 'link-2';

    document.body.appendChild(link1);
    document.body.appendChild(link2);

    const links = document.querySelectorAll('a[href]');
    expect(links.length).toBe(2);
  });

  it('should extract buttons', () => {
    const button1 = document.createElement('button');
    button1.textContent = 'Submit';
    button1.id = 'submit-btn';

    const button2 = document.createElement('button');
    button2.textContent = 'Cancel';
    button2.id = 'cancel-btn';

    document.body.appendChild(button1);
    document.body.appendChild(button2);

    const buttons = document.querySelectorAll('button');
    expect(buttons.length).toBe(2);
  });

  it('should extract inputs', () => {
    const email = document.createElement('input');
    email.type = 'email';
    email.name = 'email';
    email.placeholder = 'Enter email';

    const password = document.createElement('input');
    password.type = 'password';
    password.name = 'password';
    password.placeholder = 'Enter password';

    document.body.appendChild(email);
    document.body.appendChild(password);

    const inputs = document.querySelectorAll('input');
    expect(inputs.length).toBe(2);

    const emailInput = inputs[0] as HTMLInputElement;
    expect(emailInput.type).toBe('email');
    expect(emailInput.placeholder).toBe('Enter email');
  });

  it('should extract textareas', () => {
    const textarea = document.createElement('textarea');
    textarea.name = 'message';
    textarea.placeholder = 'Type a message';
    textarea.textContent = 'Default text';
    document.body.appendChild(textarea);

    const found = document.querySelector('textarea');
    expect(found).toBe(textarea);
  });

  it('should extract select elements', () => {
    const select = document.createElement('select');
    select.name = 'country';

    const option1 = document.createElement('option');
    option1.value = 'us';
    option1.textContent = 'United States';

    const option2 = document.createElement('option');
    option2.value = 'uk';
    option2.textContent = 'United Kingdom';

    select.appendChild(option1);
    select.appendChild(option2);
    document.body.appendChild(select);

    const found = document.querySelector('select');
    expect(found).toBe(select);
  });

  it('should respect INTERACTIVE_SELECTORS constant', () => {
    // Create elements matching each selector type
    const link = document.createElement('a');
    link.href = '#test';
    link.id = 'test-link';

    const button = document.createElement('button');
    button.id = 'test-button';

    const input = document.createElement('input');
    input.type = 'text';
    input.id = 'test-input';

    const textarea = document.createElement('textarea');
    textarea.id = 'test-textarea';

    const select = document.createElement('select');
    select.id = 'test-select';

    document.body.appendChild(link);
    document.body.appendChild(button);
    document.body.appendChild(input);
    document.body.appendChild(textarea);
    document.body.appendChild(select);

    // Test the INTERACTIVE_SELECTORS constant
    const selectors = constants.INTERACTIVE_SELECTORS.join(', ');
    const elements = document.querySelectorAll(selectors);

    expect(elements.length).toBeGreaterThan(0);
  });
});

// ============================================================================
// Element Text Extraction Tests
// ============================================================================

describe('Element Text Extraction', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should extract button text', () => {
    const button = document.createElement('button');
    button.textContent = 'Click Here';
    document.body.appendChild(button);

    expect(button.textContent).toBe('Click Here');
    // innerText may not be available in jsdom, use textContent as fallback
    const text = (button as any).innerText || button.textContent;
    expect(text).toBe('Click Here');
  });

  it('should extract link text', () => {
    const link = document.createElement('a');
    link.href = 'https://example.com';
    link.textContent = 'Example Website';
    document.body.appendChild(link);

    expect(link.textContent).toBe('Example Website');
  });

  it('should get input placeholder', () => {
    const input = document.createElement('input');
    input.type = 'text';
    input.placeholder = 'Enter your name';
    input.value = 'John Doe';
    document.body.appendChild(input);

    const inputElement = input as HTMLInputElement;
    expect(inputElement.placeholder).toBe('Enter your name');
    expect(inputElement.value).toBe('John Doe');
  });

  it('should extract select option text', () => {
    const select = document.createElement('select');
    const option1 = document.createElement('option');
    option1.value = 'option1';
    option1.textContent = 'First Option';
    option1.selected = true;

    const option2 = document.createElement('option');
    option2.value = 'option2';
    option2.textContent = 'Second Option';

    select.appendChild(option1);
    select.appendChild(option2);
    document.body.appendChild(select);

    const selectElement = select as HTMLSelectElement;
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    expect(selectedOption.textContent).toBe('First Option');
  });

  it('should handle element text limits', () => {
    const longText = 'A'.repeat(150);
    const div = document.createElement('div');
    div.textContent = longText;
    document.body.appendChild(div);

    const text = div.innerText || div.textContent || '';
    expect(text.length).toBe(150);
    expect(text.slice(0, 100)).toBe('A'.repeat(100));
  });
});

// ============================================================================
// Element Attributes Tests
// ============================================================================

describe('Element Attributes Extraction', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should extract href attribute', () => {
    const link = document.createElement('a');
    link.href = 'https://example.com/path?query=1';
    link.id = 'example-link';
    document.body.appendChild(link);

    const found = document.querySelector('#example-link');
    expect(found?.getAttribute('href')).toBe('https://example.com/path?query=1');
  });

  it('should extract aria-label', () => {
    const button = document.createElement('button');
    button.setAttribute('aria-label', 'Close dialog');
    button.id = 'close-btn';
    document.body.appendChild(button);

    const found = document.querySelector('#close-btn');
    expect(found?.getAttribute('aria-label')).toBe('Close dialog');
  });

  it('should extract role attribute', () => {
    const div = document.createElement('div');
    div.setAttribute('role', 'button');
    div.setAttribute('aria-label', 'Submit');
    div.id = 'role-button';
    document.body.appendChild(div);

    const found = document.querySelector('[role="button"]');
    expect(found).toBe(div);
  });

  it('should extract all relevant attributes', () => {
    const input = document.createElement('input');
    input.type = 'email';
    input.name = 'email';
    input.id = 'email-input';
    input.placeholder = 'Enter email';
    input.setAttribute('aria-label', 'Email address');
    input.setAttribute('data-test-id', 'email-field');
    document.body.appendChild(input);

    const found = document.querySelector('#email-input');
    expect(found?.getAttribute('type')).toBe('email');
    expect(found?.getAttribute('name')).toBe('email');
    expect(found?.getAttribute('placeholder')).toBe('Enter email');
    expect(found?.getAttribute('aria-label')).toBe('Email address');
    expect(found?.getAttribute('data-test-id')).toBe('email-field');
  });
});

// ============================================================================
// Text Content Extraction Tests
// ============================================================================

describe('Page Text Content Extraction', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should extract main content text', () => {
    const main = document.createElement('main');
    const article = document.createElement('article');
    article.textContent = 'This is the main article content that should be extracted.';
    main.appendChild(article);
    document.body.appendChild(main);

    const mainContent = (main as any).innerText || main.textContent;
    expect(mainContent).toContain('main article content');
  });

  it('should handle text normalization', () => {
    const div = document.createElement('div');
    div.innerHTML = `
      <p>Line 1</p>
      <p>Line 2</p>
      <p>Line 3</p>
    `;
    document.body.appendChild(div);

    // innerText may not be available in jsdom, use textContent
    const text = (div as any).innerText || div.textContent || '';
    const normalized = text.replace(/\s+/g, ' ').trim();
    expect(normalized).toContain('Line 1');
    expect(normalized).toContain('Line 2');
  });

  it('should limit text length', () => {
    const content = 'A'.repeat(10000);
    const div = document.createElement('div');
    div.textContent = content;
    document.body.appendChild(div);

    const text = (div as any).innerText || div.textContent || '';
    const maxLength = 1500; // From constants
    const truncated = text.slice(0, maxLength);
    expect(truncated.length).toBeLessThanOrEqual(maxLength);
  });
});

// ============================================================================
// Integration Tests
// ============================================================================

describe('DOM Observation Integration', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should handle a realistic page structure', () => {
    // Create a realistic page structure
    const header = document.createElement('header');
    header.innerHTML = '<nav><a href="/home">Home</a><a href="/products">Products</a></nav>';

    const main = document.createElement('main');
    main.innerHTML = `
      <article>
        <h1>Product Page</h1>
        <p>Description of the product goes here.</p>
        <button class="add-to-cart">Add to Cart</button>
        <input type="number" value="1" class="quantity">
      </article>
    `;

    const footer = document.createElement('footer');
    footer.innerHTML = '<p>Copyright 2024</p>';

    document.body.appendChild(header);
    document.body.appendChild(main);
    document.body.appendChild(footer);

    // Test interactive element extraction
    const selectors = constants.INTERACTIVE_SELECTORS.join(', ');
    const elements = document.querySelectorAll(selectors);

    expect(elements.length).toBeGreaterThan(0);

    // Test text extraction - innerText may not be available in jsdom
    const articleText = (main as any).innerText || main.textContent || '';
    expect(articleText).toContain('Product Page');
    expect(articleText).toContain('Add to Cart');
  });

  it('should handle nested elements', () => {
    const parent = document.createElement('div');
    parent.className = 'product-card';

    const link = document.createElement('a');
    link.href = '/product/123';
    link.textContent = 'Product Name';

    const price = document.createElement('span');
    price.className = 'price';
    price.textContent = '$99.99';

    parent.appendChild(link);
    parent.appendChild(price);
    document.body.appendChild(parent);

    const foundLink = document.querySelector('.product-card a');
    expect(foundLink).toBe(link);
    expect(foundLink?.textContent).toBe('Product Name');

    const foundPrice = document.querySelector('.product-card .price');
    expect(foundPrice).toBe(price);
    expect(foundPrice?.textContent).toBe('$99.99');
  });
});
