import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded';
import { Box, IconButton, Tooltip } from '@mui/material';
import { useState } from 'react';

export function CodeViewer({ value, label }: { value: string; label: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async (): Promise<void> => {
    await navigator.clipboard.writeText(value);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1500);
  };

  return (
    <Box sx={{ position: 'relative' }}>
      <Tooltip title={copied ? 'Copied' : 'Copy'}>
        <IconButton
          aria-label={`Copy ${label}`}
          onClick={() => void handleCopy()}
          sx={{ position: 'absolute', top: 8, right: 8, color: '#DDE7EE' }}
        >
          <ContentCopyRoundedIcon />
        </IconButton>
      </Tooltip>
      <Box
        component="pre"
        aria-label={label}
        tabIndex={0}
        sx={{
          m: 0,
          p: 2,
          pr: 7,
          overflow: 'auto',
          maxHeight: 520,
          borderRadius: 1,
          bgcolor: '#102235',
          color: '#E8F0F5',
          fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
          fontSize: '0.78rem',
          lineHeight: 1.65,
          whiteSpace: 'pre-wrap',
          overflowWrap: 'anywhere',
        }}
      >
        {value}
      </Box>
    </Box>
  );
}
