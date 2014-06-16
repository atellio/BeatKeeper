package com.bedsheetapps.tempometronome;

public enum NoteValues{
	four("4");
	
	private String noteValue;

	NoteValues(String noteValue) {
		this.noteValue = noteValue;
	}
	
	@Override public String toString() {
	    return noteValue;
	}
}
