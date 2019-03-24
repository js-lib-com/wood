$package("js.controller");

/**
 * Controller.
 */
js.controller.Controller = {
	/**
	 * Get contact.
	 *
	 * @param Function callback function to invoke on RMI completion,
	 * @param Object scope optional callback run-time scope, default to global scope.
	 * @return com.kidscademy.site.dto.Contact
	 * @assert callback is a {@link Function} and scope is an {@link Object}.
	 */
	 getContact: function() {
		var __callback__ = arguments[0];
		$assert(js.lang.Types.isFunction(__callback__), "js.controller.Controller#getContact", "Callback is not a function.");
		var __scope__ = arguments[1];
		$assert(typeof __scope__ === "undefined" || js.lang.Types.isObject(__scope__), "js.controller.Controller#getContact", "Scope is not an object.");
		if(!js.lang.Types.isObject(__scope__)) {
			__scope__ = window;
		}

		var rmi = new js.net.RMI();
		rmi.setMethod("js.controller.Controller", "getContact");
		rmi.exec(__callback__, __scope__);
	}
};
