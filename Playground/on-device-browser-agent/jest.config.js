/** @type {import('jest').Config} */
export default {
  preset: 'ts-jest/presets/js-with-ts-esm',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/tests/setup.ts'],
  moduleNameMapper: {
    '^(\\.{1,2}/.*)\\.js$': '$1',
  },
  transform: {
    '^.+\\.tsx?$': [
      'ts-jest',
      {
        useESM: true,
        tsconfig: {
          module: 'esnext',
          moduleResolution: 'bundler',
        },
      },
    ],
  },
  extensionsToTreatAsEsm: ['.ts', '.tsx'],
  testMatch: ['**/tests/**/*.test.ts', '**/tests/**/*.test.tsx'],
  collectCoverageFrom: [
    'src/content/dom-observer.ts',
    'src/content/action-executor.ts',
    'src/shared/constants.ts',
  ],
  coverageThreshold: {
    'src/content/dom-observer.ts': {
      branches: 20,
      functions: 50,
      lines: 40,
      statements: 40,
    },
    'src/content/action-executor.ts': {
      branches: 15,
      functions: 40,
      lines: 30,
      statements: 30,
    },
    'src/shared/constants.ts': {
      branches: 100,
      functions: 100,
      lines: 100,
      statements: 100,
    },
  },
  testTimeout: 10000,
};
