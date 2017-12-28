(function() {
  function suggest(urlFunc) {
    var competitionId = $(".schedule-configuration-form").data('competition-id');
    var url = urlFunc(competitionId);

    return function(partial, callback) {
      $.ajax({
        url: url,
        data: {
          q: partial
        },
        success: callback
      });
    }
  }

  var apiUrls = {
    teamSuggestionUrl: function(competitionId) {
      return "/api/v1/competition/" + competitionId + "/team-suggestions";
    },
    judgeSuggestionUrl: function(competitionId) {
      return "/api/v1/competition/" + competitionId + "/judge-suggestions";
    }
  };

  var editorViewModel = {
    'competitionId': $(".schedule-configuration-form").data('competition-id'),
    'matches': ko.observableArray(),
    'byes': [],
  };

  window.judicialManager.bindSuggestions = function() {
    function updateMatchIdField(rowElem, fieldName) {
      return function(suggestion) {
        var id = undefined;

        if (suggestion != null) {
          id = suggestion.value;
        }

        var targetIndex = $(rowElem).data('index');
        var targetObservable = editorViewModel.matches()[targetIndex][fieldName];
        targetObservable(id);
      }
    }

    $(".match-row:not(.suggesting)").each(function(index, rowElem) {
      $(rowElem).find(".prosecution-team").elemicaSuggest({
        suggestFunction: suggest(apiUrls.teamSuggestionUrl),
        valueInput: $(rowElem).find(".prosecution-team-id"),
        afterSelect: updateMatchIdField(rowElem, 'prosecutionTeamId')
      });

      $(rowElem).find(".defense-team").elemicaSuggest({
        suggestFunction: suggest(apiUrls.teamSuggestionUrl),
        valueInput: $(rowElem).find(".defense-team-id"),
        afterSelect: updateMatchIdField(rowElem, 'defenseTeamId')
      });

      $(rowElem).find(".presiding-judge").elemicaSuggest({
        suggestFunction: suggest(apiUrls.judgeSuggestionUrl),
        valueInput: $(rowElem).find(".presiding-judge-id"),
        afterSelect: updateMatchIdField(rowElem, 'presidingJudgeId')
      });

      $(rowElem).find(".scoring-judge").elemicaSuggest({
        suggestFunction: suggest(apiUrls.judgeSuggestionUrl),
        valueInput: $(rowElem).find(".scoring-judge-id"),
        afterSelect: updateMatchIdField(rowElem, 'scoringJudgeId')
      });

      $(rowElem).addClass("suggesting");
    });
  }

  judicialManager.editorViewModel = editorViewModel;

  editorViewModel.addMatch = function() {
    var newMatch = {
      prosecutionTeamName: ko.observable(""),
      prosecutionTeamId: ko.observable(undefined),
      defenseTeamName: ko.observable(""),
      defenseTeamId: ko.observable(undefined),
      presidingJudgeName: ko.observable(""),
      presidingJudgeId: ko.observable(undefined),
      scoringJudgeName: ko.observable(""),
      scoringJudgeId: ko.observable(undefined)
    }

    editorViewModel.matches.push(newMatch);
    judicialManager.bindSuggestions();
  };

  ko.applyBindings(editorViewModel, document.getElementById('schedule-editor-bindings'));
  judicialManager.bindSuggestions();
})();
