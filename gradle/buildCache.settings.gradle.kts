buildCache {
  local<DirectoryBuildCache> {
    directory = File(rootDir, ".gradle-build-cache")
    removeUnusedEntriesAfterDays = 30
  }
}