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

  var apiUrls = {
    teamSuggestionUrl: function(competitionId) {
      return "/api/v1/competition/" + competitionId + "/team-suggestions"
    }
  }

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

  function suggestTeam(teamPartial, callback) {
    var competitionId = $(".schedule-configuration-form").data('competition-id');

    $.ajax({
      url: apiUrls.teamSuggestionUrl(competitionId),
      data: {
        q: teamPartial
      },
      success: callback
    });
  }

  $(".match-row").each(function(index, rowElem) {
    $(rowElem).find(".prosecution-team").elemicaSuggest({
      suggestFunction: suggestTeam,
      valueInput: $(rowElem).find(".prosecution-team-id")
    });
  });
})();
