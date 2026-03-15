import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useApp } from '@/lib/store';
import { getRelativeTime } from '@/lib/display';
import { ArrowLeft, Edit3, Trash2, Copy, Check } from 'lucide-react';

export default function SnippetDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { getSnippet, deleteSnippet } = useApp();
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [copiedBlock, setCopiedBlock] = useState<number | null>(null);

  const snippet = getSnippet(id!);
  if (!snippet) {
    return (
      <div className="text-center py-20">
        <p className="text-sm text-[#8A9BB0]">학습 노트를 찾을 수 없습니다.</p>
        <button onClick={() => navigate('/snippets')} className="text-[#3D7A8A] text-sm mt-2 hover:underline">
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  const handleDelete = async () => {
    await deleteSnippet(snippet.id);
    navigate('/snippets');
  };

  const handleCopyBlock = (text: string, index: number) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopiedBlock(index);
      setTimeout(() => setCopiedBlock(null), 2000);
    });
  };

  // Simple markdown renderer
  const renderContent = (content: string) => {
    const lines = content.split('\n');
    const elements: JSX.Element[] = [];
    let codeBlock: string[] | null = null;
    let codeBlockLang = '';
    let codeBlockIdx = 0;

    lines.forEach((line, idx) => {
      if (line.startsWith('```')) {
        if (codeBlock !== null) {
          // Close code block
          const code = codeBlock.join('\n');
          const blockIndex = codeBlockIdx;
          elements.push(
            <div key={`code-${idx}`} className="relative group my-3">
              <div className="bg-[#1E2A3A] rounded-lg overflow-hidden">
                {codeBlockLang && (
                  <div className="px-4 py-1.5 border-b border-white/10 text-[10px] text-[#8A9BB0] font-mono">
                    {codeBlockLang}
                  </div>
                )}
                <pre className="p-4 overflow-x-auto">
                  <code className="text-[13px] font-mono text-[#E8F4F6] leading-relaxed">{code}</code>
                </pre>
              </div>
              <button
                onClick={() => handleCopyBlock(code, blockIndex)}
                className="absolute top-2 right-2 p-1.5 rounded bg-white/10 hover:bg-white/20 transition-colors opacity-0 group-hover:opacity-100"
              >
                {copiedBlock === blockIndex ? (
                  <Check className="w-3.5 h-3.5 text-[#2E7D5E]" />
                ) : (
                  <Copy className="w-3.5 h-3.5 text-white/60" />
                )}
              </button>
            </div>
          );
          codeBlock = null;
          codeBlockLang = '';
        } else {
          codeBlock = [];
          codeBlockLang = line.replace('```', '').trim();
          codeBlockIdx++;
        }
        return;
      }

      if (codeBlock !== null) {
        codeBlock.push(line);
        return;
      }

      // Headings
      if (line.startsWith('### ')) {
        elements.push(<h4 key={idx} className="text-sm font-semibold text-[#1E2A3A] mt-4 mb-2">{line.replace('### ', '')}</h4>);
      } else if (line.startsWith('## ')) {
        elements.push(<h3 key={idx} className="text-base font-semibold text-[#1E2A3A] mt-5 mb-2">{line.replace('## ', '')}</h3>);
      } else if (line.startsWith('# ')) {
        elements.push(<h2 key={idx} className="text-lg font-bold text-[#1E2A3A] mt-5 mb-3">{line.replace('# ', '')}</h2>);
      } else if (line.startsWith('| ') && line.includes('|')) {
        // Table row
        const cells = line.split('|').filter(c => c.trim()).map(c => c.trim());
        const isHeader = lines[idx + 1]?.match(/^\|[\s\-|]+\|$/);
        const isSeparator = line.match(/^\|[\s\-|]+\|$/);
        if (isSeparator) return;
        elements.push(
          <div key={idx} className="grid gap-0" style={{ gridTemplateColumns: `repeat(${cells.length}, 1fr)` }}>
            {cells.map((cell, ci) => (
              <div
                key={ci}
                className={`px-3 py-1.5 text-[13px] border-b border-[#E2E4DF] ${isHeader ? 'font-medium text-[#1E2A3A] bg-[#EFF0EC]' : 'text-[#4A5568]'}`}
              >
                {renderInline(cell)}
              </div>
            ))}
          </div>
        );
      } else if (line.startsWith('- ') || line.startsWith('* ')) {
        elements.push(
          <li key={idx} className="text-[14px] text-[#4A5568] leading-relaxed ml-4 list-disc">
            {renderInline(line.replace(/^[-*]\s/, ''))}
          </li>
        );
      } else if (line.match(/^\d+\.\s/)) {
        elements.push(
          <li key={idx} className="text-[14px] text-[#4A5568] leading-relaxed ml-4 list-decimal">
            {renderInline(line.replace(/^\d+\.\s/, ''))}
          </li>
        );
      } else if (line.trim() === '') {
        elements.push(<div key={idx} className="h-2" />);
      } else {
        elements.push(
          <p key={idx} className="text-[14px] text-[#4A5568] leading-relaxed">
            {renderInline(line)}
          </p>
        );
      }
    });

    return elements;
  };

  const renderInline = (text: string) => {
    // Handle inline code
    const parts = text.split(/(`[^`]+`)/g);
    return parts.map((part, i) => {
      if (part.startsWith('`') && part.endsWith('`')) {
        return (
          <code key={i} className="text-[13px] font-mono bg-[#EFF0EC] text-[#3D7A8A] px-1.5 py-0.5 rounded">
            {part.slice(1, -1)}
          </code>
        );
      }
      // Handle bold
      const boldParts = part.split(/(\*\*[^*]+\*\*)/g);
      return boldParts.map((bp, bi) => {
        if (bp.startsWith('**') && bp.endsWith('**')) {
          return <strong key={`${i}-${bi}`} className="font-semibold text-[#1E2A3A]">{bp.slice(2, -2)}</strong>;
        }
        return <span key={`${i}-${bi}`}>{bp}</span>;
      });
    });
  };

  return (
    <div className="pb-4 max-w-[720px]">
      {/* Back nav */}
      <button
        onClick={() => navigate('/snippets')}
        className="flex items-center gap-1.5 text-sm text-[#4A5568] hover:text-[#1E2A3A] mb-5 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        학습 노트 목록
      </button>

      {/* Title & meta */}
      <div className="flex items-start justify-between gap-4 mb-4">
        <div className="flex-1 min-w-0">
          <h1 className="text-[22px] font-bold text-[#1E2A3A] mb-2">{snippet.title}</h1>
          <div className="flex items-center gap-2 flex-wrap">
            {snippet.tags.map(tag => (
              <span key={tag} className="text-[11px] px-2.5 py-1 rounded-md bg-[#E8F4F6] text-[#3D7A8A] font-medium">
                {tag}
              </span>
            ))}
          </div>
        </div>
      </div>

      <p className="text-[12px] text-[#8A9BB0] mb-5">
        마지막 수정: {getRelativeTime(snippet.updatedAt)}
      </p>

      {/* Actions */}
      <div className="flex gap-2 mb-6">
        <button
          onClick={() => navigate(`/snippets/${snippet.id}/edit`)}
          className="h-9 px-4 rounded-lg border border-[#3D7A8A] text-[#3D7A8A] text-sm font-medium hover:bg-[#E8F4F6] active:scale-[0.97] transition-all flex items-center gap-2"
        >
          <Edit3 className="w-3.5 h-3.5" />
          수정
        </button>
        {showDeleteConfirm ? (
          <div className="flex items-center gap-2">
            <span className="text-sm text-[#B91C1C]">정말 삭제하시겠습니까?</span>
            <button
              onClick={handleDelete}
              className="h-9 px-3 rounded-lg bg-[#B91C1C] text-white text-sm font-medium hover:bg-[#991B1B] transition-all"
            >
              확인
            </button>
            <button
              onClick={() => setShowDeleteConfirm(false)}
              className="h-9 px-3 rounded-lg border border-[#E2E4DF] text-[#4A5568] text-sm hover:bg-[#EFF0EC] transition-all"
            >
              취소
            </button>
          </div>
        ) : (
          <button
            onClick={() => setShowDeleteConfirm(true)}
            className="h-9 px-4 rounded-lg border border-[#B91C1C]/30 text-[#B91C1C] text-sm font-medium hover:bg-[#FEE2E2] transition-all flex items-center gap-2"
          >
            <Trash2 className="w-3.5 h-3.5" />
            삭제
          </button>
        )}
      </div>

      {/* Content */}
      <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 md:p-6">
        {renderContent(snippet.content)}
      </div>
    </div>
  );
}
