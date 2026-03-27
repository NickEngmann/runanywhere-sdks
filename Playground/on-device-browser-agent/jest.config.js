export default {
  testEnvironment: "node",
  transform: {
    "^.+\\.(ts|tsx)$": "ts-jest"
  },
  moduleFileExtensions: ["ts", "tsx", "js", "jsx"],
  testMatch: ["**/*.test.ts", "**/*.test.tsx"],
  collectCoverageFrom: [
    "src/**/*.ts",
    "src/**/*.tsx"
  ],
  coverageReporters: ["text", "lcov", "html"],
  testPathIgnorePatterns: [
    "/node_modules/",
    "/repo_cache/"
  ],
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1"
  }
};
