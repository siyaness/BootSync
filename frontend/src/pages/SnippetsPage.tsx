import { useMemo } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useApp } from '@/lib/store';
import { getAllTags, getRelativeTime } from '@/lib/display';
import { compareApiDateTimesDesc } from '@/lib/seoul-time';
import { Search, Plus, Code2, X } from 'lucide-react';
import { useIsMobile } from '@/hooks/use-mobile';
import { cn } from '@/lib/utils';

export default function SnippetsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { snippets } = useApp();
  const isMobile = useIsMobile();
  const searchQuery = searchParams.get('q') || '';
  const selectedTag = searchParams.get('tag')?.trim() || null;

  const allTags = useMemo(() => getAllTags(snippets), [snippets]);

  const updateFilters = (nextQuery: string, nextTag: string | null) => {
    const nextParams = new URLSearchParams();
    const normalizedQuery = nextQuery.trim();

    if (normalizedQuery) {
      nextParams.set('q', normalizedQuery);
    }

    if (nextTag) {
      nextParams.set('tag', nextTag);
    }

    setSearchParams(nextParams, { replace: true });
  };

  const filtered = useMemo(() => {
    const normalizedQuery = searchQuery.trim();
    let items = [...snippets].sort((a, b) => compareApiDateTimesDesc(a.updatedAt, b.updatedAt));

    if (selectedTag) {
      items = items.filter(s => s.tags.includes(selectedTag));
    }

    if (normalizedQuery) {
      const q = normalizedQuery.toLowerCase();
      items = items.filter(
        s =>
          s.title.toLowerCase().includes(q) ||
          s.content.toLowerCase().includes(q) ||
          s.tags.some(t => t.toLowerCase().includes(q))
      );
    }

    return items;
  }, [snippets, searchQuery, selectedTag]);

  const clearSearch = () => {
    updateFilters('', null);
  };

  return (
    <div className="space-y-5 pb-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-[22px] font-bold text-[#1E2A3A]">내 학습 노트</h1>
        {!isMobile && (
          <button
            onClick={() => navigate('/snippets/new')}
            className="h-9 px-4 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            새 학습 노트
          </button>
        )}
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#8A9BB0]" />
        <input
          type="text"
          value={searchQuery}
          onChange={e => updateFilters(e.target.value, selectedTag)}
          className="w-full h-10 pl-10 pr-4 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20 transition-all"
          placeholder="제목 또는 내용으로 검색..."
        />
        {searchQuery && (
          <button
            onClick={() => updateFilters('', selectedTag)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8A9BB0] hover:text-[#4A5568]"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Tag filter */}
      {allTags.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
          <button
            onClick={() => updateFilters(searchQuery, null)}
            className={cn(
              'shrink-0 px-3 py-1.5 rounded-lg text-[12px] font-medium transition-all',
              !selectedTag
                ? 'bg-[#3D7A8A] text-white'
                : 'bg-[#EFF0EC] text-[#4A5568] hover:bg-[#E2E4DF]'
            )}
          >
            전체
          </button>
          {allTags.map(tag => (
            <button
              key={tag}
              onClick={() => updateFilters(searchQuery, tag === selectedTag ? null : tag)}
              className={cn(
                'shrink-0 px-3 py-1.5 rounded-lg text-[12px] font-medium transition-all',
                selectedTag === tag
                  ? 'bg-[#3D7A8A] text-white'
                  : 'bg-[#EFF0EC] text-[#4A5568] hover:bg-[#E2E4DF]'
              )}
            >
              {tag}
            </button>
          ))}
        </div>
      )}

      {/* Study notes list */}
      {snippets.length === 0 ? (
        /* Empty state — no study notes at all */
        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-8 text-center">
          <div className="w-16 h-16 rounded-2xl bg-[#EFF0EC] flex items-center justify-center mx-auto mb-4">
            <Code2 className="w-7 h-7 text-[#8A9BB0]" />
          </div>
          <p className="text-base font-medium text-[#1E2A3A] mb-1">아직 저장된 학습 노트가 없습니다.</p>
          <p className="text-sm text-[#8A9BB0] mb-5">
            명령어, 설정 방법, 오류 해결법 등을 저장해보세요.
          </p>
          <button
            onClick={() => navigate('/snippets/new')}
            className="h-10 px-5 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all mx-auto flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            첫 학습 노트 만들기
          </button>
        </div>
      ) : filtered.length === 0 ? (
        /* Search empty state */
        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-8 text-center">
          <p className="text-sm text-[#4A5568] mb-3">
            '{searchQuery || selectedTag}'에 대한 결과가 없습니다.
          </p>
          <button
            onClick={clearSearch}
            className="h-9 px-4 rounded-lg border border-[#E2E4DF] text-[#4A5568] text-sm font-medium hover:bg-[#EFF0EC] transition-all"
          >
            검색 초기화
          </button>
        </div>
      ) : (
        <div className={cn('grid gap-3', !isMobile && 'grid-cols-2')}>
          {filtered.map(snippet => (
            <Link
              key={snippet.id}
              to={`/snippets/${snippet.id}`}
              className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-4 hover:shadow-[0_4px_12px_rgba(30,42,58,0.08)] transition-all block"
            >
              <h3 className="text-[15px] font-semibold text-[#1E2A3A] mb-2 truncate">{snippet.title}</h3>
              <div className="flex items-center gap-1.5 mb-2 flex-wrap">
                {snippet.tags.slice(0, 3).map(tag => (
                  <span key={tag} className="text-[10px] px-2 py-0.5 rounded bg-[#E8F4F6] text-[#3D7A8A] font-medium">
                    {tag}
                  </span>
                ))}
                {snippet.tags.length > 3 && (
                  <span className="text-[10px] px-2 py-0.5 rounded bg-[#EFF0EC] text-[#8A9BB0] font-medium">
                    +{snippet.tags.length - 3}
                  </span>
                )}
              </div>
              <p className="text-[13px] text-[#8A9BB0] line-clamp-2 leading-relaxed mb-2">
                {snippet.content.replace(/[#`*\-|]/g, '').substring(0, 120)}...
              </p>
              <p className="text-[11px] text-[#8A9BB0]">{getRelativeTime(snippet.updatedAt)}</p>
            </Link>
          ))}
        </div>
      )}

      {/* Mobile FAB */}
      {isMobile && snippets.length > 0 && (
        <button
          onClick={() => navigate('/snippets/new')}
          className="fixed bottom-20 right-4 w-14 h-14 rounded-full bg-[#3D7A8A] text-white shadow-[0_4px_12px_rgba(30,42,58,0.2)] hover:bg-[#346A78] active:scale-95 transition-all flex items-center justify-center z-30"
        >
          <Plus className="w-6 h-6" />
        </button>
      )}
    </div>
  );
}
