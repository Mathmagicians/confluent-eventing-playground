@manual
Feature: A release ships what is ready
  A release groups the features it releases. It ships those that are Spec green, on their passing BDD tests or a
  check played by hand, and the others move on to the next release.

  Rule: A release ships its rows in Verification, and the others move to the next release's catalog

    Scenario: The merge ships the features in Verification
      Given 0.0.6 with features in Verification
      When it merges into main
      Then their rows read Archive, signed In production in the verification file

    Scenario: A feature not ready moves on
      Given a feature in Discovery at the merge of 0.0.6
      When its row moves to the catalog of 0.0.7
      Then its card lies in the swimlane 0.0.7

  Rule: A feature we decide is not ready carries @wip, and make bdd leaves it out

    Scenario: The features run but @wip and @manual
      Given a feature file tagged @wip and one tagged @manual
      When I run make bdd
      Then every scenario plays but theirs

    Scenario: A feature not ready leaves the release green
      Given a feature we decided is not ready for the release, tagged @wip
      When the release's checks run
      Then they are green without it

    Scenario: The snippets cover the features not ready
      Given a scenario tagged @wip with an undefined step
      When I run make bdd-snippets
      Then its snippet is printed

  Rule: A check played by hand carries @manual, and its evidence is in the phase file

    Scenario: A check played by hand stays out of the run and the snippets
      Given the talk of the Slides spike as a scenario tagged @manual
      When I run make bdd and make bdd-snippets
      Then neither plays it, nor prints its snippets

    Scenario: The evidence of a check played by hand
      Given themathmagician played the talk
      When Spec green is signed
      Then the automation file says what themathmagician saw, with the date
