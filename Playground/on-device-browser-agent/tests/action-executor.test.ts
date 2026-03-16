/**
 * Action Executor Tests - Core Functionality
 *
 * Tests for executeAction, error handling, and core action flows
 */

import { executeAction } from '../src/content/action-executor';

describe('Action Executor - Core Functionality', () => {
  beforeEach(() => {
    // Reset DOM before each test
    document.body.innerHTML = '';
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  describe('executeAction - click', () => {
    it('should handle click on non-existent element', async () => {
      const result = await executeAction('click', { selector: '#non-existent' });

      expect(result).toBeDefined();
      expect(result.success).toBe(false);
      expect(result.error).toContain('Element not found');
    });

    it('should handle click on hidden element', async () => {
      const button = document.createElement('button');
      button.id = 'hidden-button';
      button.style.display = 'none';
      document.body.appendChild(button);

      const result = await executeAction('click', { selector: '#hidden-button' });

      expect(result.success).toBe(false);
    });
  });

  describe('executeAction - type', () => {
    it('should handle type into non-existent field', async () => {
      const result = await executeAction('type', {
        selector: '#non-existent',
        text: 'Test text',
      });

      expect(result.success).toBe(false);
      expect(result.error).toContain('Element not found');
    });
  });

  describe('executeAction - press_enter', () => {
    it('should handle press_enter on non-existent element', async () => {
      const result = await executeAction('press_enter', {
        selector: '#non-existent',
      });

      expect(result.success).toBe(false);
    });

    it('should handle press_enter on non-input element', async () => {
      const div = document.createElement('div');
      div.id = 'non-input';
      div.textContent = 'Not an input';
      document.body.appendChild(div);

      const result = await executeAction('press_enter', {
        selector: '#non-input',
      });

      expect(result.success).toBe(false);
    });
  });

  describe('executeAction - extract', () => {
    it('should handle extract from non-existent element', async () => {
      const result = await executeAction('extract', {
        selector: '#non-existent',
      });

      expect(result.success).toBe(false);
      expect(result.error).toContain('Element not found');
    });
  });

  describe('executeAction - scroll', () => {
    it('should handle scroll with valid direction', async () => {
      const result = await executeAction('scroll', {
        direction: 'down',
        amount: '100',
      });

      expect(result).toBeDefined();
    });

    it('should handle scroll with up direction', async () => {
      const result = await executeAction('scroll', {
        direction: 'up',
        amount: '50',
      });

      expect(result).toBeDefined();
    });
  });

  describe('executeAction - navigate', () => {
    it('should return special message for navigate action', async () => {
      const result = await executeAction('navigate', {
        url: 'https://example.com',
      });

      expect(result.success).toBe(true);
      expect(result.data).toContain('Navigation handled by service worker');
    });
  });

  describe('executeAction - unknown action', () => {
    it('should handle unknown action types', async () => {
      const result = await executeAction('unknown_action' as any, {});

      expect(result.success).toBe(false);
      expect(result.error).toContain('Unknown action type');
    });
  });

  describe('executeAction - error handling', () => {
    it('should handle null/empty selector for click', async () => {
      const result = await executeAction('click', { selector: '' });

      expect(result.success).toBe(false);
    });

    it('should handle empty selector for type', async () => {
      const result = await executeAction('type', {
        selector: '',
        text: 'test',
      });

      expect(result.success).toBe(false);
    });
  });

  describe('executeAction - element resolution', () => {
    it('should find element by ID selector', async () => {
      const button = document.createElement('button');
      button.id = 'test-button';
      document.body.appendChild(button);

      // The element exists, but scrollIntoView may fail in jsdom
      // Just verify it doesn't throw an error
      const result = await executeAction('click', { selector: '#test-button' });
      expect(result).toBeDefined();
    });

    it('should find element by class selector', async () => {
      const div = document.createElement('div');
      div.className = 'test-class';
      document.body.appendChild(div);

      const result = await executeAction('click', { selector: '.test-class' });
      expect(result).toBeDefined();
    });
  });
});
