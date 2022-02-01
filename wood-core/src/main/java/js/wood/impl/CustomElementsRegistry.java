package js.wood.impl;

import java.util.HashMap;
import java.util.Map;

import js.wood.CompoPath;
import js.wood.ICustomElement;
import js.wood.ICustomElementsRegistry;

public class CustomElementsRegistry implements ICustomElementsRegistry {
	private final Map<String, ICustomElement> registry = new HashMap<>();

	@Override
	public void register(String tagName, CompoPath compoPath, String operator) {
		registry.put(tagName, new CustomElement(compoPath.value(), operator));
	}

	@Override
	public ICustomElement getByTagName(String tagName) {
		return registry.get(tagName);
	}

	private class CustomElement implements ICustomElement {
		private final String compoPath;
		private final String operator;

		public CustomElement(String compoPath, String operator) {
			this.compoPath = compoPath;
			this.operator = operator;
		}

		@Override
		public String compoPath() {
			return compoPath;
		}

		@Override
		public String operator() {
			return operator;
		}
	}
}
