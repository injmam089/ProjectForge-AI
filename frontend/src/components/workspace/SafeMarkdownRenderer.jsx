import React from 'react';

/**
 * SafeMarkdownRenderer
 *
 * Zero-dependency, pure React Markdown renderer.
 * Strictly avoids `dangerouslySetInnerHTML`.
 * Parses CommonMark-like blocks and inlines safely into standard React elements.
 */

// Helper to sanitize link URLs (only allow safe schemes)
function isSafeUrl(url) {
  if (!url) return false;
  const trimmed = url.trim().toLowerCase();
  return (
    trimmed.startsWith('https://') ||
    trimmed.startsWith('http://') ||
    trimmed.startsWith('#') ||
    trimmed.startsWith('/')
  );
}

// Inline token parser (renders bold, italic, inline code, links, text)
function renderInline(text) {
  if (!text) return null;

  const elements = [];
  let remaining = text;
  let key = 0;

  // Regex patterns for inline markup
  // 1: `code`
  // 2: **bold** or __bold__
  // 3: *italic* or _italic_
  // 4: [text](url)
  const inlineRegex = /(`[^`]+`|\*\*[^*]+\*\*|__[^_]+__|(?<!\*)\*[^*]+(?!\*)|(?<!_)_[^_]+(?<!_)|\[([^\]]+)\]\(([^)]+)\))/;

  while (remaining.length > 0) {
    const match = remaining.match(inlineRegex);
    if (!match) {
      elements.push(<React.Fragment key={key++}>{remaining}</React.Fragment>);
      break;
    }

    const matchIndex = match.index;
    if (matchIndex > 0) {
      elements.push(<React.Fragment key={key++}>{remaining.slice(0, matchIndex)}</React.Fragment>);
    }

    const matchedStr = match[0];

    if (matchedStr.startsWith('`') && matchedStr.endsWith('`')) {
      const codeContent = matchedStr.slice(1, -1);
      elements.push(
        <code
          key={key++}
          style={{
            background: 'rgba(99, 102, 241, 0.12)',
            color: '#a5b4fc',
            padding: '2px 6px',
            borderRadius: '4px',
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
            fontSize: '0.88em',
            border: '1px solid rgba(99, 102, 241, 0.25)',
          }}
        >
          {codeContent}
        </code>
      );
    } else if (
      (matchedStr.startsWith('**') && matchedStr.endsWith('**')) ||
      (matchedStr.startsWith('__') && matchedStr.endsWith('__'))
    ) {
      const boldContent = matchedStr.slice(2, -2);
      elements.push(
        <strong key={key++} style={{ color: '#f8fafc', fontWeight: 700 }}>
          {renderInline(boldContent)}
        </strong>
      );
    } else if (
      (matchedStr.startsWith('*') && matchedStr.endsWith('*')) ||
      (matchedStr.startsWith('_') && matchedStr.endsWith('_'))
    ) {
      const italicContent = matchedStr.slice(1, -1);
      elements.push(
        <em key={key++} style={{ fontStyle: 'italic', color: '#e2e8f0' }}>
          {renderInline(italicContent)}
        </em>
      );
    } else if (matchedStr.startsWith('[') && matchedStr.includes('](')) {
      const linkText = match[2];
      const linkUrl = match[3];
      if (isSafeUrl(linkUrl)) {
        elements.push(
          <a
            key={key++}
            href={linkUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              color: '#818cf8',
              textDecoration: 'underline',
              textUnderlineOffset: '3px',
              transition: 'color 0.15s ease',
            }}
          >
            {renderInline(linkText)}
          </a>
        );
      } else {
        elements.push(<React.Fragment key={key++}>{linkText}</React.Fragment>);
      }
    }

    remaining = remaining.slice(matchIndex + matchedStr.length);
  }

  return elements;
}

export function SafeMarkdownRenderer({ content }) {
  if (!content) {
    return (
      <div style={{ color: '#64748b', fontStyle: 'italic', padding: '16px 0' }}>
        No content to preview.
      </div>
    );
  }

  const lines = content.split('\n');
  const blocks = [];
  let i = 0;
  let blockKey = 0;

  while (i < lines.length) {
    const line = lines[i];

    // Fenced Code Block: ```[lang]
    if (line.trim().startsWith('```')) {
      const lang = line.trim().slice(3).trim();
      const codeLines = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        codeLines.push(lines[i]);
        i++;
      }
      i++; // Skip closing fence
      blocks.push(
        <div
          key={blockKey++}
          style={{
            margin: '16px 0',
            borderRadius: '8px',
            overflow: 'hidden',
            border: '1px solid #334155',
            background: '#090d16',
          }}
        >
          {lang && (
            <div
              style={{
                padding: '6px 14px',
                background: '#131b2e',
                color: '#94a3b8',
                fontSize: '0.72rem',
                textTransform: 'uppercase',
                letterSpacing: '0.06em',
                fontFamily: 'ui-monospace, monospace',
                borderBottom: '1px solid #1e293b',
              }}
            >
              {lang}
            </div>
          )}
          <pre
            style={{
              margin: 0,
              padding: '16px',
              overflowX: 'auto',
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
              fontSize: '0.85rem',
              lineHeight: 1.55,
              color: '#e2e8f0',
            }}
          >
            <code>{codeLines.join('\n')}</code>
          </pre>
        </div>
      );
      continue;
    }

    // Markdown Table: line contains '|'
    if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
      const tableLines = [];
      while (i < lines.length && lines[i].trim().startsWith('|') && lines[i].trim().endsWith('|')) {
        tableLines.push(lines[i].trim());
        i++;
      }

      if (tableLines.length >= 2) {
        // First row is header, second is delimiter
        const headerRow = tableLines[0]
          .split('|')
          .slice(1, -1)
          .map((c) => c.trim());
        const dataRows = tableLines.slice(2).map((r) =>
          r
            .split('|')
            .slice(1, -1)
            .map((c) => c.trim())
        );

        blocks.push(
          <div
            key={blockKey++}
            style={{
              margin: '20px 0',
              overflowX: 'auto',
              border: '1px solid #334155',
              borderRadius: '8px',
            }}
          >
            <table
              style={{
                width: '100%',
                borderCollapse: 'collapse',
                fontSize: '0.88rem',
                textAlign: 'left',
              }}
            >
              <thead>
                <tr style={{ background: '#1e293b', borderBottom: '1px solid #334155' }}>
                  {headerRow.map((cell, idx) => (
                    <th
                      key={idx}
                      style={{
                        padding: '10px 14px',
                        color: '#f8fafc',
                        fontWeight: 600,
                        borderRight: idx < headerRow.length - 1 ? '1px solid #334155' : 'none',
                      }}
                    >
                      {renderInline(cell)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {dataRows.map((row, rIdx) => (
                  <tr
                    key={rIdx}
                    style={{
                      background: rIdx % 2 === 0 ? '#0f172a' : '#141e33',
                      borderBottom: rIdx < dataRows.length - 1 ? '1px solid #1e293b' : 'none',
                    }}
                  >
                    {row.map((cell, cIdx) => (
                      <td
                        key={cIdx}
                        style={{
                          padding: '8px 14px',
                          color: '#cbd5e1',
                          borderRight: cIdx < row.length - 1 ? '1px solid #1e293b' : 'none',
                        }}
                      >
                        {renderInline(cell)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
        continue;
      }
    }

    // Blank line
    if (line.trim() === '') {
      i++;
      continue;
    }

    // Headings: #, ##, ###, ####, #####, ######
    const headingMatch = line.match(/^(#{1,6})\s+(.*)$/);
    if (headingMatch) {
      const level = headingMatch[1].length;
      const headingText = headingMatch[2];
      const HeadingTag = `h${level}`;

      const headingStyles = {
        h1: { fontSize: '1.75rem', fontWeight: 800, color: '#f8fafc', marginTop: '28px', marginBottom: '14px', borderBottom: '1px solid #334155', paddingBottom: '8px' },
        h2: { fontSize: '1.4rem', fontWeight: 700, color: '#f1f5f9', marginTop: '24px', marginBottom: '12px', borderBottom: '1px solid rgba(51, 65, 85, 0.5)', paddingBottom: '6px' },
        h3: { fontSize: '1.18rem', fontWeight: 600, color: '#e2e8f0', marginTop: '20px', marginBottom: '10px' },
        h4: { fontSize: '1.02rem', fontWeight: 600, color: '#cbd5e1', marginTop: '16px', marginBottom: '8px' },
        h5: { fontSize: '0.92rem', fontWeight: 600, color: '#94a3b8', marginTop: '14px', marginBottom: '6px' },
        h6: { fontSize: '0.85rem', fontWeight: 600, color: '#64748b', marginTop: '12px', marginBottom: '4px', textTransform: 'uppercase' },
      };

      blocks.push(
        <HeadingTag key={blockKey++} style={headingStyles[HeadingTag]}>
          {renderInline(headingText)}
        </HeadingTag>
      );
      i++;
      continue;
    }

    // Blockquote: > text
    if (line.trim().startsWith('>')) {
      const quoteLines = [];
      while (i < lines.length && lines[i].trim().startsWith('>')) {
        quoteLines.push(lines[i].trim().replace(/^>\s?/, ''));
        i++;
      }
      blocks.push(
        <blockquote
          key={blockKey++}
          style={{
            margin: '16px 0',
            padding: '10px 16px',
            borderLeft: '4px solid #6366f1',
            background: 'rgba(99, 102, 241, 0.08)',
            borderRadius: '0 6px 6px 0',
            color: '#cbd5e1',
            fontStyle: 'italic',
          }}
        >
          {quoteLines.map((qLine, qIdx) => (
            <div key={qIdx} style={{ margin: qIdx > 0 ? '4px 0 0 0' : 0 }}>
              {renderInline(qLine)}
            </div>
          ))}
        </blockquote>
      );
      continue;
    }

    // Horizontal Rule: --- or ***
    if (line.trim() === '---' || line.trim() === '***' || line.trim() === '___') {
      blocks.push(
        <hr
          key={blockKey++}
          style={{
            margin: '24px 0',
            border: 'none',
            borderTop: '1px solid #334155',
          }}
        />
      );
      i++;
      continue;
    }

    // Unordered List: - or *
    if (line.trim().match(/^[-*]\s+/)) {
      const listItems = [];
      while (i < lines.length && lines[i].trim().match(/^[-*]\s+/)) {
        const itemText = lines[i].trim().replace(/^[-*]\s+/, '');
        listItems.push(itemText);
        i++;
      }
      blocks.push(
        <ul
          key={blockKey++}
          style={{
            margin: '12px 0 16px 20px',
            paddingLeft: '12px',
            color: '#cbd5e1',
            lineHeight: 1.65,
          }}
        >
          {listItems.map((item, lIdx) => (
            <li key={lIdx} style={{ marginBottom: '6px' }}>
              {renderInline(item)}
            </li>
          ))}
        </ul>
      );
      continue;
    }

    // Ordered List: \d+\.
    if (line.trim().match(/^\d+\.\s+/)) {
      const listItems = [];
      while (i < lines.length && lines[i].trim().match(/^\d+\.\s+/)) {
        const itemText = lines[i].trim().replace(/^\d+\.\s+/, '');
        listItems.push(itemText);
        i++;
      }
      blocks.push(
        <ol
          key={blockKey++}
          style={{
            margin: '12px 0 16px 20px',
            paddingLeft: '12px',
            color: '#cbd5e1',
            lineHeight: 1.65,
          }}
        >
          {listItems.map((item, lIdx) => (
            <li key={lIdx} style={{ marginBottom: '6px' }}>
              {renderInline(item)}
            </li>
          ))}
        </ol>
      );
      continue;
    }

    // Standard Paragraph
    const paraLines = [];
    while (
      i < lines.length &&
      lines[i].trim() !== '' &&
      !lines[i].trim().startsWith('#') &&
      !lines[i].trim().startsWith('```') &&
      !lines[i].trim().startsWith('>') &&
      !lines[i].trim().startsWith('|') &&
      !lines[i].trim().match(/^[-*]\s+/) &&
      !lines[i].trim().match(/^\d+\.\s+/) &&
      lines[i].trim() !== '---'
    ) {
      paraLines.push(lines[i]);
      i++;
    }

    blocks.push(
      <p
        key={blockKey++}
        style={{
          margin: '0 0 14px 0',
          color: '#cbd5e1',
          lineHeight: 1.7,
          fontSize: '0.94rem',
        }}
      >
        {renderInline(paraLines.join(' '))}
      </p>
    );
  }

  return (
    <div
      className="markdown-rendered-view"
      style={{
        color: '#cbd5e1',
        fontSize: '0.94rem',
        lineHeight: 1.68,
      }}
    >
      {blocks}
    </div>
  );
}
