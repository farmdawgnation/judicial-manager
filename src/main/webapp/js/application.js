(function() {
  window.judicialManager = {
    event: function(eventName, parameters) {
      var event = $.Event(eventName, parameters);
      $(document).trigger(event);
    }
  };

  var eventNames = {
    ajaxServicingStarted: "ajax-servicing-started",
    ajaxServicingEnded: "ajax-servicing-ended"
  };

  $(document).on(eventNames.ajaxServicingStarted, function(event) {
    $("form").find("input, button, select").attr("disabled", "");
  });

  $(document).on(eventNames.ajaxServicingEnded, function(event) {
    $("form").find("input, button, select").removeAttr("disabled");
  });

  $("#select-competition-container").on('click', '.competition-entry', function(event) {
    var $target = $(event.target);

    if (! $target.is(".select-competition")) {
      var href = $(event.target)
        .closest(".competition-entry")
        .find(".select-competition")
        .attr('href');

      window.location.href = href;
    }
  });
})();
