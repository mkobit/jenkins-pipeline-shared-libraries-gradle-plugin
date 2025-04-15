buildCache {
  local {
    directory = File(rootDir, ".gradle-build-cache")
    removeUnusedEntriesAfterDays = 30
  }
}
