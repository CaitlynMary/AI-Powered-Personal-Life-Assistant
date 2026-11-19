import { TestBed } from '@angular/core/testing';
import { NoteResponse } from '../data/note.models';
import { NoteEditor } from './note-editor';

describe('NoteEditor', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('requires non-blank title and content and enforces backend length limits', () => {
    const fixture = TestBed.createComponent(NoteEditor);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);

    component.form.setValue({ title: '   ', content: '   ' });
    component.submit();
    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.title.touched).toBe(true);
    expect(component.form.controls.content.touched).toBe(true);
    expect(save).not.toHaveBeenCalled();

    component.form.controls.title.setValue('x'.repeat(201));
    component.form.controls.content.setValue('A note');
    expect(component.form.controls.title.hasError('maxlength')).toBe(true);
  });

  it('trims and submits the actual note request, and keeps view mode read-only', () => {
    const fixture = TestBed.createComponent(NoteEditor);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({ title: '  A thought  ', content: '  Keep this close.  ' });
    component.submit();
    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        title: 'A thought',
        content: 'Keep this close.',
        noteType: 'GENERAL',
        journalDate: null,
      },
    });

    const note: NoteResponse = {
      id: 4,
      title: 'Saved thought',
      content: 'A saved idea.',
      noteType: 'GENERAL',
      journalDate: null,
      pinned: true,
      createdAt: '2026-11-12T09:00:00',
      updatedAt: '2026-11-12T09:00:00',
    };
    fixture.componentRef.setInput('note', note);
    fixture.componentRef.setInput('mode', 'VIEW');
    fixture.detectChanges();
    component.submit();
    expect(save).toHaveBeenCalledTimes(1);
    const titleInput = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(titleInput.value).toBe('Saved thought');
    expect(titleInput.readOnly).toBe(true);

    fixture.componentRef.setInput('note', {
      ...note,
      noteType: 'JOURNAL',
      journalDate: '2026-11-11',
    });
    fixture.componentRef.setInput('mode', 'EDIT');
    fixture.detectChanges();
    component.form.setValue({ title: 'Edited journal', content: 'Keep its date.' });
    component.submit();
    expect(save).toHaveBeenLastCalledWith({
      id: 4,
      request: {
        title: 'Edited journal',
        content: 'Keep its date.',
        noteType: 'JOURNAL',
        journalDate: '2026-11-11',
      },
    });
  });
});
