$package("js.widget");

/**
 * Client side CAPTCHA implementation. Because this class is in fact a widget it relies on HTML code. It needs an
 * element to display CAPTCHA challenge value and an images container; they both are identified by CSS class <b>value</b>
 * respective <b>images</b>. There is also a clickable element with CSS class <b>load-challenge</b> used to load
 * another challenge in case user is not able to solve the riddle.
 * <p>
 * Here is the reference CAPTCHA layout implementation. Fieldset hosting CAPTCHA layout may have <code>data-name</code>
 * attribute. If present, CAPTCHA is automatically tested for validity by parent form and <code>validity</code> CSS
 * class mark updated accordingly.
 * 
 * <pre>
 *  &lt;form&gt;
 *      &lt;fieldset&gt;
 *          ...
 *      &lt;/fieldset&gt;
 *      
 *      &lt;fieldset data-name="captcha" data-class="js.widget.Captcha"&gt;
 *          &lt;p&gt;Click on &lt;span class="value"&gt;astronaut&lt;/span&gt; or &lt;a class="load-challenge"&gt;load&lt;/a&gt; another set.&lt;/p&gt;
 *          &lt;div class="images"&gt;&lt;img src="astronaut.png" /&gt;...&lt;img src="bag.png"/&gt;&lt;/div&gt;
 *      &lt;/fieldset&gt;
 *  &lt;/form&gt;
 * </pre>
 * 
 * <p>
 * Deprecated implementation: challenge response should be send from client together with data to protect. Current
 * solution enables the form submission if challenge response is recognized by server but client can reverse engineer
 * and send the form bypassing form validation.
 * 
 * @author Iulian Rotaru
 */
js.widget.Captcha = class extends HTMLElement {
	/**
	 * CAPTCHA instance index seed.
	 * @type {Number}
	 */
	static _INDEX_SEED = 0;

	/**
	 * Relative URL for CAPTCHA challenge load.
	 * @type {String}
	 */
	//static _SERVER_LOAD_CHALLENGE = "captcha/challenge";
	static _SERVER_LOAD_CHALLENGE = "js/tiny/container/http/captcha/Captcha/getChallenge.rmi";

	/**
	 * Relative URL for CAPTCHA response verification.
	 * @type {String}
	 */
	//static _SERVER_LOAD_RESPONSE = "captcha/verify";
	static _SERVER_VERIFY_RESPONSE = "js/tiny/container/http/captcha/Captcha/verifyResponse.rmi";

	/**
	 * Mark CSS class to identify invalid state.
	 * @type {String}
	 */
	static _CSS_INVALID = "invalid";

	/**
	 * Mark CSS class to identify selected challenge element.
	 * @type {String}
	 */
	static _CSS_SELECTED = "selected";

	constructor() {
		super();

		/**
		 * CAPTCHA instance index.
		 * @type {Number}
		 */
		this._index = js.widget.Captcha._INDEX_SEED++;

		/**
		 * Callback for asynchronous {@link #isCorrect}.
		 * @type {Function}
		 */
		this._callback = null;

		/**
		 * Callback run-time execution scope. Optional, default to global scope.
		 * @type {Object}
		 */
		this._scope = null;

		/**
		 * Challenge value container. It is an element with CSS class <em>value</em> used by this class to display
		 * challenge text value.
		 * @type {HTMLElement}
		 */
		this._value = this.getElementsByClassName("value")[0];

		/**
		 * Challenge images container. It is an element with CSS class <em>images</em> used by this class to display
		 * challenge images.
		 * @type {HTMLElement}
		 */
		this._images = this.getElementsByClassName("images")[0];
		while (this._images.firstChild) { this._images.removeChild(this._images.lastChild); }

		/**
		 * Validity state of this CAPTCHA instance, updated every time user select a challenge image. This value is then
		 * returned by {@link #isValid()}.
		 * @type {Boolean}
		 */
		this._valid = false;

		this.getElementsByClassName("load-challenge")[0].addEventListener("click", this._loadChallenge.bind(this));
		//this._loadChallenge();
	}

	/**
	 * Override control implementation to return only <code>data-name</code> name.
	 * @return {String} CAPTCHA control name.
	 */
	getName() {
		return this.getAttribute("data-name");
	}

	/**
	 * CAPTCHA setter does nothing.
	 * @return {js.widget.Captcha} this object;
	 */
	setValue() {
		return this;
	}

	/**
	 * CAPTCHA has no value to return.
	 * @return {Object} always return null.
	 */
	getValue() {
		return null;
	}

	/**
	 * Reset CAPTCHA control.
	 * @return {js.widget.Captcha} this object.
	 */
	reset() {
		this.classList.remove(js.widget.Captcha._CSS_INVALID);
		[...this.getElementsByTagName("img")].forEach(img => img.classList.remove(js.widget.Captcha._CSS_SELECTED));
		this._valid = false;
		return this;
	}

	/**
	 * Return this CAPTCHA validity state and update CSS mark class. Validity state, see {@link #_valid} is updated
	 * every time user select a challenge image. This method simply return cached value.
	 * @return {Boolean} true if selected image is the correct one.
	 * @see #_valid
	 */
	isValid() {
		this.classList.toggle(js.widget.Captcha._CSS_INVALID, !this._valid);
		return this._valid;
	}

	/**
	 * On image click extract challenge token attached to image and test its correctness on server side logic. Challenge
	 * response is verified by {@link #_onResponseVerified(Object)}.
	 */
	_onCaptchaImageClick(ev) {
		this.classList.remove(js.widget.Captcha._CSS_INVALID);

		const images = this._images.getElementsByTagName("img");
		for (let i = 0; i < images.length; ++i) {
			images.item(i).classList.remove(js.widget.Captcha._CSS_SELECTED);
		}

		const img = ev.target;
		img.classList.add(js.widget.Captcha._CSS_SELECTED);

		// SRC is something like ...resource.xsp?token=983aa14ed173493b9f08b41dd4592e6e
		// response is token that identify the image, i.e. last 32 chars
		const src = img.src;
		const response = src.substr(src.length - 32);

		fetch(js.widget.Captcha._SERVER_VERIFY_RESPONSE, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify([this._index, response])
		}).then(response => response.json()).then(challenge => this._onResponseVerified(challenge));
	}

	/**
	 * Callback invoked by server side challenge response verify. This method gets challenge validity result from
	 * server: if challenge response was correct server returns null. If challenge response was not correct server
	 * returns another challenge to give user another try.
	 * @param {Object} challenge new challenge if response was rejected and null if accepted.
	 */
	_onResponseVerified(challenge) {
		// new challenge is sent back by server signal that response was wrong and need to reload challenge
		// if challenge is null response was correct
		// in both cases remove invalid CSS mark class
		this.classList.remove(js.widget.Captcha._CSS_INVALID);

		if (challenge) {
			this._valid = false;
			this._onChallengeLoaded(challenge);
		}
		else {
			this._valid = true;
		}
	}

	/**
	 * Handler for user request to load another challenge. This method is executed asynchronously and the actual
	 * challenge is loaded into user interface by {@link #_onChallengeLoaded}.
	 */
	_loadChallenge(ev) {
		if (typeof ev !== "undefined") {
			ev.preventDefault();
			ev.stopPropagation();
		}
		fetch(js.widget.Captcha._SERVER_LOAD_CHALLENGE, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(this._index)
		}).then(response => response.json()).then(challenge => this._onChallengeLoaded(challenge));
	}

	/**
	 * Load another challenge on user interface. Challenge is provided by server side logic on user request to load new
	 * challenge or when select an incorrect response.
	 */
	_onChallengeLoaded(challenge) {
		this._value.textContent = challenge.value;
		while (this._images.firstChild) { this._images.removeChild(this._images.lastChild); }

		for (let i = 0, images = challenge.images; i < images.length; i++) {
			const img = document.createElement("img");
			img.src = images[i];
			img.addEventListener("click", this._onCaptchaImageClick.bind(this));
			this._images.appendChild(img);
		}
	}
};

customElements.define("js-captcha", js.widget.Captcha);
