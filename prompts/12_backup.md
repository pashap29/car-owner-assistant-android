Goal:
Implement local backup creation and rotation.

Context:
Read backup rules, storage strategy, and files/media docs.

Constraints:
- Local backup only.
- No folder picker in MVP.
- Keep only backup-new and backup-old.
- Backup composition is controlled by settings.
- Do not implement cloud backup.

Done when:
- backup can be created from settings
- backup rotation works correctly
- only two backups are retained
- status of last backup is visible
- tests cover rotation behavior

Output format:
1. files changed
2. implemented logic
3. tests added
4. verification steps
5. risks