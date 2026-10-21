@manual
Feature: The board follows the catalog
  A release plan's catalog holds its features, a row each. Every row is an issue and a card on the project board,
  in the row's state, in the swimlane of its release, with what the phase files say about the feature.

  Rule: Each feature in a release's catalog has an issue, linked in its row

    Scenario: A row without an issue gets one
      Given a catalog row with an empty Issue cell
      When I sync the board
      Then the sync creates the issue, titled as the feature
      And the issue's link is written into the row

    Scenario: A row with its issue keeps it
      Given a catalog row with its issue linked
      When I sync the board
      Then the sync creates no issue

    Scenario: Main receives the links through the pull request
      Given a pull request with every catalog row linked
      When it merges into main
      Then main holds every link, and no sync pushes to main

    Scenario: A release is a swimlane
      Given the release plans under docs/releases
      When I sync the board
      Then every row has a card, and every release is a swimlane with no card of its own

  Rule: An issue's body is the feature's sections of the phase files

    Scenario: The body has a section per phase
      Given Load on demand has a section in the discovery, formulation, and automation files
      When I sync the board
      Then its card shows them under Discovery, Formulation, and Automation
      And their links point into the synced branch

    Scenario: A phase without a section is left out
      Given One name has no section in the formulation file
      When I sync the board
      Then its card shows Discovery and Automation

    Scenario: The files win over an edit on GitHub
      Given an issue's body edited on GitHub
      When I sync the board
      Then the body is written from the files again

  Rule: An issue's Phase is its row's state, its milestone the version in its overview's file name

    Scenario: A signed gate moves the card
      Given a row in Automation
      When I sign Spec green and sync the board
      Then the card moves to Verification

    Scenario: A planned release has its swimlane
      Given a feature in release-0.0.7/release-0.0.7.md
      When I sync the board
      Then its card lies in the swimlane 0.0.7

    Scenario: A missing milestone is created
      Given a release whose version has no milestone
      When I sync the board
      Then the sync creates the milestone, without a due date

    Scenario: A state matches its Phase option in any case
      Given a row in Automation and the Phase option automation
      When I sync the board
      Then the card is in automation

    Scenario: A state without an option stops the sync
      Given a row in Parked and no Phase option Parked
      When I sync the board
      Then the sync stops, naming Parked
