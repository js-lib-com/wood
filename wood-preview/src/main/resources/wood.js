document.addEventListener("DOMContentLoaded", function() {
	var eventSource = new EventSource("wood.event");
	eventSource.addEventListener("FileSystemEvent", function(event) {
		event = JSON.parse(event.data);
		console.log(event.file + ":" + event.action);
		if (document.visibilityState == "visible") {
			location.reload();
		}
	});
});