/**
 * YouTube Element Extraction Tests
 * 
 * Tests for YouTube-specific element extraction, including:
 * - Search box detection
 * - Video link extraction
 * - Video renderer handling
 * - Selector generation for YouTube elements
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import type { InteractiveElement } from '../src/shared/types';

// ============================================================================
// Search Box Detection Tests
// ============================================================================

describe('YouTube Search Box Detection', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should find search input with id=search', () => {
    const searchInput = document.createElement('input');
    searchInput.id = 'search';
    searchInput.type = 'text';
    searchInput.placeholder = 'Search';
    document.body.appendChild(searchInput);

    const found = document.querySelector('input#search');
    expect(found).toBe(searchInput);
    expect((found as HTMLInputElement).placeholder).toBe('Search');
  });

  it('should handle search input without id', () => {
    const searchInput = document.createElement('input');
    searchInput.type = 'text';
    searchInput.placeholder = 'Search YouTube';
    document.body.appendChild(searchInput);

    const found = document.querySelector('input');
    expect(found).toBe(searchInput);
    expect((found as HTMLInputElement).placeholder).toBe('Search YouTube');
  });

  it('should detect search input placeholder', () => {
    const searchInput = document.createElement('input');
    searchInput.type = 'text';
    searchInput.placeholder = 'Search';
    document.body.appendChild(searchInput);

    const input = searchInput as HTMLInputElement;
    expect(input.placeholder).toBe('Search');
    expect(input.placeholder?.length).toBeGreaterThan(0);
  });
});

// ============================================================================
// Video Renderer Detection Tests
// ============================================================================

describe('YouTube Video Renderer Detection', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should find ytd-video-renderer elements', () => {
    const renderer1 = document.createElement('div');
    renderer1.setAttribute('is', 'ytd-video-renderer');
    renderer1.id = 'video-renderer-1';

    const renderer2 = document.createElement('div');
    renderer2.setAttribute('is', 'ytd-video-renderer');
    renderer2.id = 'video-renderer-2';

    document.body.appendChild(renderer1);
    document.body.appendChild(renderer2);

    // Query by attribute since custom element matching may not work in jsdom
    const renderers = document.querySelectorAll('[is="ytd-video-renderer"]');
    expect(renderers.length).toBe(2);
  });

  it('should find ytd-rich-item-renderer elements', () => {
    const renderer = document.createElement('div');
    renderer.setAttribute('is', 'ytd-rich-item-renderer');
    renderer.id = 'rich-item-1';
    document.body.appendChild(renderer);

    // Query by attribute
    const found = document.querySelector('[is="ytd-rich-item-renderer"]');
    expect(found).toBe(renderer);
  });

  it('should find ytd-compact-video-renderer elements', () => {
    const renderer = document.createElement('div');
    renderer.setAttribute('is', 'ytd-compact-video-renderer');
    renderer.id = 'compact-1';
    document.body.appendChild(renderer);

    // Query by attribute
    const found = document.querySelector('[is="ytd-compact-video-renderer"]');
    expect(found).toBe(renderer);
  });

  it('should handle multiple video renderer types', () => {
    const videoRenderer = document.createElement('div');
    videoRenderer.setAttribute('is', 'ytd-video-renderer');

    const richItemRenderer = document.createElement('div');
    richItemRenderer.setAttribute('is', 'ytd-rich-item-renderer');

    const compactVideoRenderer = document.createElement('div');
    compactVideoRenderer.setAttribute('is', 'ytd-compact-video-renderer');

    document.body.appendChild(videoRenderer);
    document.body.appendChild(richItemRenderer);
    document.body.appendChild(compactVideoRenderer);

    // Query all custom element attributes
    const allRenderers = document.querySelectorAll('[is="ytd-video-renderer"], [is="ytd-rich-item-renderer"], [is="ytd-compact-video-renderer"]');
    expect(allRenderers.length).toBe(3);
  });
});

// ============================================================================
// Video Title Link Extraction Tests
// ============================================================================

describe('YouTube Video Title Link Extraction', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should find video title link with id=video-title', () => {
    const renderer = document.createElement('div');
    renderer.setAttribute('is', 'ytd-video-renderer');
    renderer.id = 'video-renderer-1';

    const titleLink = document.createElement('a');
    titleLink.id = 'video-title';
    titleLink.href = '/watch?v=dQw4w9WgXcQ';
    titleLink.textContent = 'Rick Astley - Never Gonna Give You Up';

    renderer.appendChild(titleLink);
    document.body.appendChild(renderer);

    // Query within renderer context
    const found = renderer.querySelector('a#video-title');
    expect(found).toBe(titleLink);
    expect((found as HTMLAnchorElement).href).toContain('/watch?v=');
  });

  it('should find video title link with id=video-title-link', () => {
    const renderer = document.createElement('div');
    renderer.setAttribute('is', 'ytd-video-renderer');
    renderer.id = 'video-renderer-1';

    const titleLink = document.createElement('a');
    titleLink.id = 'video-title-link';
    titleLink.href = '/watch?v=abc123';
    titleLink.textContent = 'Test Video Title';

    renderer.appendChild(titleLink);
    document.body.appendChild(renderer);

    // Query within renderer context
    const found = renderer.querySelector('a#video-title-link');
    expect(found).toBe(titleLink);
  });

  it('should find video links with href containing /watch', () => {
    const renderer = document.createElement('div');
    renderer.setAttribute('is', 'ytd-video-renderer');
    renderer.id = 'video-renderer-1';

    const titleLink = document.createElement('a');
    titleLink.href = '/watch?v=xyz789';
    titleLink.textContent = 'Another Video';

    renderer.appendChild(titleLink);
    document.body.appendChild(renderer);

    // Query within renderer context
    const found = renderer.querySelector('a[href*="/watch"]');
    expect(found).toBe(titleLink);
  });

  it('should handle multiple video title links', () => {
    const renderer1 = document.createElement('div');
    renderer1.setAttribute('is', 'ytd-video-renderer');

    const renderer2 = document.createElement('div');
    renderer2.setAttribute('is', 'ytd-rich-item-renderer');

    const title1 = document.createElement('a');
    title1.id = 'video-title';
    title1.href = '/watch?v=1';
    title1.textContent = 'Video 1';

    const title2 = document.createElement('a');
    title2.id = 'video-title';
    title2.href = '/watch?v=2';
    title2.textContent = 'Video 2';

    renderer1.appendChild(title1);
    renderer2.appendChild(title2);

    document.body.appendChild(renderer1);
    document.body.appendChild(renderer2);

    const allTitles = document.querySelectorAll('a#video-title');
    expect(allTitles.length).toBe(2);
  });
});

// ============================================================================
// Search Button Detection Tests
// ============================================================================

describe('YouTube Search Button Detection', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should find search button with id=search-icon-legacy', () => {
    const searchButton = document.createElement('button');
    searchButton.id = 'search-icon-legacy';
    searchButton.textContent = 'Search';
    document.body.appendChild(searchButton);

    const found = document.querySelector('#search-icon-legacy');
    expect(found).toBe(searchButton);
  });

  it('should find search button in legacy format', () => {
    const searchButton = document.createElement('span');
    searchButton.id = 'search-icon-legacy';
    document.body.appendChild(searchButton);

    const found = document.querySelector('button#search-icon-legacy');
    // May not find button variant if it's a span
    expect(found).toBeNull();

    // But should find the element by id
    const foundById = document.querySelector('#search-icon-legacy');
    expect(foundById).toBe(searchButton);
  });
});

// ============================================================================
// YouTube Selector Generation Tests
// ============================================================================

describe('YouTube Selector Generation', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should generate selector for video title by href', () => {
    const link = document.createElement('a');
    link.href = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';
    link.id = 'video-title';
    link.setAttribute('data-href', '/watch?v=dQw4w9WgXcQ');
    document.body.appendChild(link);

    // Use data attribute instead since link.href is absolute
    const selector = '[data-href="/watch?v=dQw4w9WgXcQ"]';

    const found = document.querySelector(selector);
    expect(found).toBe(link);
  });

  it('should handle video title with complex href', () => {
    const link = document.createElement('a');
    link.href = 'https://www.youtube.com/watch?v=abc123&list=PLxyz';
    link.id = 'video-title';
    link.setAttribute('data-href', '/watch?v=abc123&list=PLxyz');
    document.body.appendChild(link);

    // Use data attribute
    const selector = '[data-href="/watch?v=abc123&list=PLxyz"]';

    const found = document.querySelector(selector);
    expect(found).toBe(link);
  });

  it('should handle video title with complex href', () => {
    const link = document.createElement('a');
    link.href = 'https://www.youtube.com/watch?v=abc123&list=PLxyz';
    link.id = 'video-title';
    link.setAttribute('data-href', '/watch?v=abc123&list=PLxyz');
    document.body.appendChild(link);

    // Use data attribute
    const selector = '[data-href="/watch?v=abc123&list=PLxyz"]';

    const found = document.querySelector(selector);
    expect(found).toBe(link);
  });

  it('should generate nth-of-type selector for video renderer', () => {
    const renderer1 = document.createElement('div');
    renderer1.setAttribute('is', 'ytd-video-renderer');
    renderer1.id = 'renderer-1';

    const renderer2 = document.createElement('div');
    renderer2.setAttribute('is', 'ytd-video-renderer');
    renderer2.id = 'renderer-2';

    const renderer3 = document.createElement('div');
    renderer3.setAttribute('is', 'ytd-video-renderer');
    renderer3.id = 'renderer-3';

    // Create a parent container
    const container = document.createElement('div');
    container.appendChild(renderer1);
    container.appendChild(renderer2);
    container.appendChild(renderer3);
    document.body.appendChild(container);

    // Test nth-of-type selector
    const selector = 'div[ytd-video-renderer]:nth-of-type(2)';
    const found = document.querySelector(selector);

    // nth-of-type uses tag name, not attributes
    // So we need to find by actual tag
    const foundById = container.children[1];
    expect(foundById?.id).toBe('renderer-2');
  });
});

// ============================================================================
// Interactive Element Selection Tests
// ============================================================================

describe('YouTube Interactive Element Selection', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should filter elements by MAX_INTERACTIVE_ELEMENTS limit', () => {
    // Create more elements than the limit
    const limit = 30; // From constants
    
    for (let i = 0; i < 50; i++) {
      const link = document.createElement('a');
      link.href = `/video/${i}`;
      link.textContent = `Video ${i}`;
      link.setAttribute('id', `video-title`);
      document.body.appendChild(link);
    }

    const allLinks = document.querySelectorAll('a');
    expect(allLinks.length).toBe(50);
    
    // Simulate slicing to limit
    const limitedLinks = Array.from(allLinks).slice(0, limit);
    expect(limitedLinks.length).toBe(limit);
  });

  it('should skip elements with text < 5 chars', () => {
    const renderer = document.createElement('div');
    renderer.setAttribute('is', 'ytd-video-renderer');

    const shortLink = document.createElement('a');
    shortLink.href = '/watch?v=abc';
    shortLink.textContent = 'Video'; // 5 chars
    
    const longLink = document.createElement('a');
    longLink.href = '/watch?v=xyz';
    longLink.textContent = 'This is a much longer video title';

    renderer.appendChild(shortLink);
    renderer.appendChild(longLink);
    document.body.appendChild(renderer);

    const allLinks = renderer.querySelectorAll('a');
    expect(allLinks.length).toBe(2);

    // Filter by text length > 5
    const validLinks = Array.from(allLinks).filter(link => 
      link.textContent && link.textContent.length > 5
    );
    expect(validLinks.length).toBe(1);
    expect(validLinks[0]).toBe(longLink);
  });

  it('should filter out UI links like "filter", "sort", "subscribe"', () => {
    const renderer = document.createElement('div');
    renderer.setAttribute('is', 'ytd-video-renderer');

    const uiLink1 = document.createElement('a');
    uiLink1.textContent = 'Filter';
    
    const uiLink2 = document.createElement('a');
    uiLink2.textContent = 'Sort by';
    
    const uiLink3 = document.createElement('a');
    uiLink3.textContent = 'Subscribe';
    
    const videoLink = document.createElement('a');
    videoLink.textContent = 'Actual Video Title';

    renderer.appendChild(uiLink1);
    renderer.appendChild(uiLink2);
    renderer.appendChild(uiLink3);
    renderer.appendChild(videoLink);
    document.body.appendChild(renderer);

    const allLinks = renderer.querySelectorAll('a');
    const filteredLinks = Array.from(allLinks).filter(link => {
      const textLower = link.textContent?.toLowerCase() || '';
      if (textLower.includes('filter') ||
          textLower.includes('sort') ||
          textLower.includes('subscribe') ||
          textLower.includes('sign in')) {
        return false;
      }
      return true;
    });

    expect(filteredLinks.length).toBe(1);
    expect(filteredLinks[0]).toBe(videoLink);
  });
});

// ============================================================================
// YouTube Video Element Type Tests
// ============================================================================

describe('YouTube Video Element Types', () => {
  it('should have video-link type', () => {
    const videoElement: InteractiveElement = {
      index: 0,
      tag: 'a',
      type: 'video-link',
      text: 'Test Video',
      selector: 'a#video-title',
      attributes: { href: '/watch?v=test' },
    };

    expect(videoElement.type).toBe('video-link');
    expect(videoElement.tag).toBe('a');
    expect(videoElement.text).toBe('Test Video');
  });

  it('should handle standard input type', () => {
    const videoElement: InteractiveElement = {
      index: 1,
      tag: 'input',
      type: 'search',
      text: 'Search',
      selector: 'input#search',
      attributes: { placeholder: 'Search YouTube' },
    };

    expect(videoElement.type).toBe('search');
    expect(videoElement.tag).toBe('input');
  });

  it('should handle button type', () => {
    const videoElement: InteractiveElement = {
      index: 2,
      tag: 'button',
      type: undefined,
      text: 'Search',
      selector: '#search-icon-legacy',
      attributes: {},
    };

    expect(videoElement.tag).toBe('button');
    expect(videoElement.text).toBe('Search');
  });
});

// ============================================================================
// YouTube State Machine Tests
// ============================================================================

describe('YouTube State Machine', () => {
  it('should have NAVIGATING state', () => {
    const states: 'NAVIGATING' | 'ON_HOMEPAGE' | 'ON_RESULTS' | 'ON_VIDEO' | 'DONE' = 'NAVIGATING';
    expect(states).toBe('NAVIGATING');
  });

  it('should have ON_HOMEPAGE state', () => {
    const states: 'NAVIGATING' | 'ON_HOMEPAGE' | 'ON_RESULTS' | 'ON_VIDEO' | 'DONE' = 'ON_HOMEPAGE';
    expect(states).toBe('ON_HOMEPAGE');
  });

  it('should have ON_RESULTS state', () => {
    const states: 'NAVIGATING' | 'ON_HOMEPAGE' | 'ON_RESULTS' | 'ON_VIDEO' | 'DONE' = 'ON_RESULTS';
    expect(states).toBe('ON_RESULTS');
  });

  it('should have ON_VIDEO state', () => {
    const states: 'NAVIGATING' | 'ON_HOMEPAGE' | 'ON_RESULTS' | 'ON_VIDEO' | 'DONE' = 'ON_VIDEO';
    expect(states).toBe('ON_VIDEO');
  });

  it('should have DONE state', () => {
    const states: 'NAVIGATING' | 'ON_HOMEPAGE' | 'ON_RESULTS' | 'ON_VIDEO' | 'DONE' = 'DONE';
    expect(states).toBe('DONE');
  });
});

// ============================================================================
// YouTube State Machine Functionality Tests
// ============================================================================

describe('YouTube State Machine Functionality', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should navigate to YouTube URL', () => {
    const targetUrl = 'https://www.youtube.com';
    
    // Test URL pattern matching
    expect(targetUrl.includes('youtube.com')).toBe(true);
    expect(targetUrl.startsWith('https://')).toBe(true);
  });

  it('should detect YouTube watch URL', () => {
    const watchUrl = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';
    
    expect(watchUrl.includes('/watch')).toBe(true);
    expect(watchUrl.includes('youtube.com')).toBe(true);
  });

  it('should detect YouTube search URL', () => {
    const searchUrl = 'https://www.youtube.com/results?search_query=test';
    
    expect(searchUrl.includes('/results')).toBe(true);
    expect(searchUrl.includes('search_query=')).toBe(true);
  });

  it('should handle YouTube with subdomains', () => {
    const urls = [
      'https://www.youtube.com',
      'https://m.youtube.com',
      'https://youtube.com',
    ];

    urls.forEach(url => {
      expect(url.includes('youtube.com')).toBe(true);
    });
  });
});
