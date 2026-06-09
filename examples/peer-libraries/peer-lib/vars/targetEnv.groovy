def call(String branch) {
    if (branch == 'main') return 'prod'
    if (branch.startsWith('release/')) return 'staging'
    return 'dev'
}
