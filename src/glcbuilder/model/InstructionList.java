package model;

import java.lang.Iterable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Collection;

public class InstructionList implements Iterable<Instruction> {
	private Collection<Instruction> delegate = new Vector<>();

	public void add(Instruction el) {
		delegate.add(el);
	}

	@Override
	public Iterator<Instruction> iterator() {
		return delegate.iterator();
	}
}
