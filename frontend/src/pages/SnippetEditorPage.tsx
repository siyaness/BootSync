import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AppApiError } from '@/lib/api';
import { useApp } from '@/lib/store';
import { getAllTags } from '@/lib/display';
import { ArrowLeft, X, AlertTriangle, Bold, Code2, List, Braces } from 'lucide-react';
import { cn } from '@/lib/utils';

type WarningPayload = {
  message?: string;
  secretWarningToken?: string;
};

export default function SnippetEditorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { getSnippet, addSnippet, updateSnippet, snippets } = useApp();
  const isEditing = Boolean(id);

  const existingSnippet = isEditing ? getSnippet(id!) : null;
  const existingTags = useMemo(() => getAllTags(snippets), [snippets]);

  const [title, setTitle] = useState(existingSnippet?.title || '');
  const [tags, setTags] = useState<string[]>(existingSnippet?.tags || []);
  const [tagInput, setTagInput] = useState('');
  const [content, setContent] = useState(existingSnippet?.content || '');
  const [showTagSuggestions, setShowTagSuggestions] = useState(false);
  const [isTagComposing, setIsTagComposing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [warningMessage, setWarningMessage] = useState<string | null>(null);
  const [warningToken, setWarningToken] = useState('');
  const [confirmSave, setConfirmSave] = useState(false);

  useEffect(() => {
    if (!existingSnippet) return;
    setTitle(existingSnippet.title);
    setTags(existingSnippet.tags);
    setContent(existingSnippet.content);
  }, [existingSnippet]);

  if (isEditing && !existingSnippet) {
    return (
      <div className="text-center py-20">
        <p className="text-sm text-[#8A9BB0]">수정할 학습 노트를 찾을 수 없습니다.</p>
        <button onClick={() => navigate('/snippets')} className="text-[#3D7A8A] text-sm mt-2 hover:underline">
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  const handleAddTag = (tag: string) => {
    const trimmed = tag.trim().toLowerCase();
    if (!trimmed) return false;

    let added = false;
    setTags(prev => {
      if (prev.includes(trimmed) || prev.length >= 10) {
        return prev;
      }
      added = true;
      return [...prev, trimmed];
    });

    if (added) {
      setTagInput('');
      setShowTagSuggestions(false);
    }

    return added;
  };

  const handleRemoveTag = (tag: string) => {
    setTags(tags.filter(t => t !== tag));
  };

  const handleTagKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (isTagComposing || e.nativeEvent.isComposing) {
      return;
    }

    if (e.key === 'Enter' || e.key === ',' || e.key === 'Tab') {
      e.preventDefault();
      handleAddTag(tagInput);
    }
    if (e.key === 'Backspace' && !tagInput && tags.length > 0) {
      setTags(tags.slice(0, -1));
    }
  };

  const filteredSuggestions = existingTags.filter(
    t => t.includes(tagInput.toLowerCase()) && !tags.includes(t)
  );

  const handleSave = async () => {
    if (!title.trim() || !content.trim()) return;

    const pendingTag = tagInput.trim().toLowerCase();
    const nextTags = pendingTag && !tags.includes(pendingTag) && tags.length < 10
      ? [...tags, pendingTag]
      : tags;

    if (pendingTag && nextTags !== tags) {
      setTags(nextTags);
      setTagInput('');
      setShowTagSuggestions(false);
    }

    setSaving(true);
    setSaveError(null);

    try {
      if (isEditing && existingSnippet) {
        await updateSnippet(existingSnippet.id, { title: title.trim(), tags: nextTags, content }, warningToken);
        navigate(`/snippets/${existingSnippet.id}`);
      } else {
        await addSnippet({ title: title.trim(), tags: nextTags, content }, warningToken);
        navigate('/snippets');
      }
    } catch (error) {
      if (error instanceof AppApiError && error.code === 'secret_warning_required') {
        const payload = (error.payload || {}) as WarningPayload;
        setWarningMessage(payload.message || '민감 정보로 보이는 내용이 감지되었습니다. 같은 내용으로 다시 저장해야 합니다.');
        setWarningToken(payload.secretWarningToken || '');
        setConfirmSave(true);
      } else if (error instanceof AppApiError) {
        setSaveError(error.message);
      } else {
        setSaveError('학습 노트를 저장하지 못했습니다.');
      }
    } finally {
      setSaving(false);
    }
  };

  const insertMarkdown = (type: 'bold' | 'code' | 'codeblock' | 'list') => {
    const textarea = document.getElementById('snippet-body') as HTMLTextAreaElement | null;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selected = content.substring(start, end);

    let insert = '';
    switch (type) {
      case 'bold': insert = `**${selected || '텍스트'}**`; break;
      case 'code': insert = `\`${selected || '코드'}\``; break;
      case 'codeblock': insert = `\n\`\`\`\n${selected || '코드를 입력하세요'}\n\`\`\`\n`; break;
      case 'list': insert = `\n- ${selected || '항목'}`; break;
    }

    const newContent = content.substring(0, start) + insert + content.substring(end);
    setContent(newContent);
    setConfirmSave(false);
    setWarningToken('');
    setWarningMessage(null);
  };

  return (
    <div className="pb-4 max-w-[720px]">
      <button
        onClick={() => navigate(isEditing ? `/snippets/${id}` : '/snippets')}
        className="flex items-center gap-1.5 text-sm text-[#4A5568] hover:text-[#1E2A3A] mb-5 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        {isEditing ? '돌아가기' : '학습 노트 목록'}
      </button>

      <h1 className="text-[22px] font-bold text-[#1E2A3A] mb-5">
        {isEditing ? '학습 노트 수정' : '새 학습 노트'}
      </h1>

      <div className="space-y-5">
        <div>
          <input
            type="text"
            value={title}
            onChange={e => {
              setTitle(e.target.value);
              setConfirmSave(false);
            }}
            className="w-full text-xl font-semibold text-[#1E2A3A] placeholder:text-[#C8CCC6] bg-transparent border-none outline-none py-2"
            placeholder="학습 노트 제목을 입력하세요"
          />
          <div className="h-px bg-[#E2E4DF]" />
        </div>

        <div className="space-y-1.5">
          <label className="text-sm font-medium text-[#4A5568]">태그 (최대 10개)</label>
          <div className="flex items-center flex-wrap gap-1.5 p-2.5 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] focus-within:border-[#3D7A8A] focus-within:ring-2 focus-within:ring-[#3D7A8A]/20 transition-all min-h-[40px]">
            {tags.map(tag => (
              <span key={tag} className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-[#E8F4F6] text-[#3D7A8A] text-[12px] font-medium">
                {tag}
                <button onClick={() => handleRemoveTag(tag)} className="hover:text-[#B91C1C]">
                  <X className="w-3 h-3" />
                </button>
              </span>
            ))}
            {tags.length < 10 && (
              <div className="relative flex-1 min-w-[100px]">
                <input
                  type="text"
                  value={tagInput}
                  onChange={e => { setTagInput(e.target.value); setShowTagSuggestions(true); }}
                  onKeyDown={handleTagKeyDown}
                  onCompositionStart={() => setIsTagComposing(true)}
                  onCompositionEnd={() => setIsTagComposing(false)}
                  onFocus={() => setShowTagSuggestions(true)}
                  onBlur={() => setTimeout(() => setShowTagSuggestions(false), 200)}
                  className="w-full bg-transparent text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] outline-none"
                  placeholder={tags.length === 0 ? '태그 입력 후 Enter 또는 쉼표' : ''}
                />
                {showTagSuggestions && tagInput && filteredSuggestions.length > 0 && (
                  <div className="absolute top-full left-0 mt-1 w-full bg-white border border-[#E2E4DF] rounded-lg shadow-[0_4px_12px_rgba(30,42,58,0.08)] z-10 max-h-32 overflow-y-auto">
                    {filteredSuggestions.map(tag => (
                      <button
                        key={tag}
                        onMouseDown={() => handleAddTag(tag)}
                        className="w-full text-left px-3 py-2 text-sm text-[#4A5568] hover:bg-[#EFF0EC] transition-colors"
                      >
                        {tag}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center gap-1 border border-[#E2E4DF] rounded-lg p-1 bg-[#FDFCFB] w-fit">
          <button onClick={() => insertMarkdown('bold')} className="p-2 rounded hover:bg-[#EFF0EC] transition-colors text-[#4A5568]" title="Bold">
            <Bold className="w-4 h-4" />
          </button>
          <button onClick={() => insertMarkdown('code')} className="p-2 rounded hover:bg-[#EFF0EC] transition-colors text-[#4A5568]" title="Inline code">
            <Code2 className="w-4 h-4" />
          </button>
          <button onClick={() => insertMarkdown('codeblock')} className="p-2 rounded hover:bg-[#EFF0EC] transition-colors text-[#4A5568]" title="Code block">
            <Braces className="w-4 h-4" />
          </button>
          <button onClick={() => insertMarkdown('list')} className="p-2 rounded hover:bg-[#EFF0EC] transition-colors text-[#4A5568]" title="List">
            <List className="w-4 h-4" />
          </button>
        </div>

        <div className="space-y-1.5">
          <textarea
            id="snippet-body"
            value={content}
            onChange={e => {
              setContent(e.target.value);
              setConfirmSave(false);
              setWarningToken('');
              setWarningMessage(null);
            }}
            className="w-full min-h-[300px] px-4 py-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-[13px] font-mono text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20 resize-y transition-all leading-relaxed"
            placeholder="명령어, 설정 단계, 오류 해결법 등을 자유롭게 작성하세요. 마크다운 형식을 지원합니다."
          />
          <p className="text-[11px] text-[#8A9BB0] text-right">{content.length}자</p>
        </div>

        {warningMessage && (
          <div className="bg-[#FEF3C7] border border-[#D97706]/20 rounded-xl p-4 space-y-3">
            <div className="flex items-start gap-2.5">
              <AlertTriangle className="w-5 h-5 text-[#D97706] shrink-0 mt-0.5" />
              <p className="text-sm text-[#92400E] leading-relaxed">
                {warningMessage}
              </p>
            </div>
            <p className="text-[12px] text-[#92400E]">
              같은 내용으로 다시 저장하면 경고를 확인한 것으로 처리됩니다.
            </p>
          </div>
        )}

        {saveError && (
          <div className="bg-[#FEE2E2] border border-[#B91C1C]/20 rounded-xl p-4 text-sm text-[#991B1B]">
            {saveError}
          </div>
        )}

        <div className="sticky bottom-0 bg-[#F7F6F3] py-3 -mx-1 px-1">
          <button
            onClick={handleSave}
            disabled={saving || !title.trim() || !content.trim()}
            className={cn(
              'w-full h-11 rounded-lg text-sm font-medium transition-all flex items-center justify-center gap-2',
              confirmSave
                ? 'bg-[#D97706] text-white hover:bg-[#B45309]'
                : 'bg-[#3D7A8A] text-white hover:bg-[#346A78]',
              'active:scale-[0.97] disabled:opacity-50 disabled:cursor-not-allowed'
            )}
          >
            {saving ? '저장 중...' : confirmSave ? '경고를 확인하고 저장하기' : '저장'}
          </button>
        </div>
      </div>
    </div>
  );
}
