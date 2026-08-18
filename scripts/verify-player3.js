const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const root = path.resolve(__dirname, '..');

function collectJavaScriptFiles(targetPath) {
  if (!fs.existsSync(targetPath)) {
    return [];
  }

  const stat = fs.statSync(targetPath);

  if (stat.isFile()) {
    return targetPath.endsWith('.js') ? [targetPath] : [];
  }

  return fs.readdirSync(targetPath, { withFileTypes: true }).flatMap((entry) =>
    collectJavaScriptFiles(path.join(targetPath, entry.name))
  );
}

function runNode(args, label) {
  const result = spawnSync(process.execPath, args, {
    cwd: root,
    stdio: 'inherit',
  });

  if (result.status !== 0) {
    throw new Error(`${label} failed.`);
  }
}

function syntaxCheck() {
  const files = [
    path.join(root, 'server.js'),
    ...collectJavaScriptFiles(
      path.join(root, 'src/modules/assignments')
    ),
    ...collectJavaScriptFiles(
      path.join(root, 'src/modules/search')
    ),
    ...collectJavaScriptFiles(path.join(root, 'scripts')).filter(
      (file) => path.basename(file).startsWith('verify-player3')
    ),
  ];

  const uniqueFiles = [...new Set(files)].sort();

  for (const file of uniqueFiles) {
    runNode(['--check', file], `Syntax check for ${path.relative(root, file)}`);
  }

  console.log(
    `Player 3 syntax verification PASSED (${uniqueFiles.length} files)`
  );
}

function runBatchVerifiers() {
  const verifiers = [
    'scripts/verify-player3-batch-b.js',
    'scripts/verify-player3-batch-c.js',
    'scripts/verify-player3-batch-d.js',
    'scripts/verify-player3-batch-e.js',
  ];

  for (const verifier of verifiers) {
    const fullPath = path.join(root, verifier);

    if (!fs.existsSync(fullPath)) {
      throw new Error(`Required verifier is missing: ${verifier}`);
    }

    runNode([fullPath], verifier);
  }
}

try {
  syntaxCheck();
  runBatchVerifiers();

  console.log('');
  console.log('Player 3 full independent verification PASSED');
  console.log(
    'Database/auth end-to-end checks remain deferred until Player 1 integration.'
  );
} catch (err) {
  console.error('');
  console.error('Player 3 full independent verification FAILED');
  console.error(err.message);
  process.exit(1);
}
