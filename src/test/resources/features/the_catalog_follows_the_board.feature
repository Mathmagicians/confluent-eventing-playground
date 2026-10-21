@manual
Feature: The catalog follows the board
  A member of Mathmagicians moves a card on the project board, and the release plan on the release's branch says
  so: the row's state and the date of the gate the move signs.

  Rule: A feature's phase is the issue field Phase, and the board's columns are its options

    Scenario: The board has a column per phase
      Given the issue field Phase with discovery, formulation, automation, verification, parking, and archive
      When I open the board
      Then it has a column per option

  Rule: A member of Mathmagicians moves the board's cards

    Scenario: A member moves a card
      Given I am a member of Mathmagicians with push access
      When I set a card's Phase
      Then the release plan follows

    Scenario: The board is private
      Given I am signed out
      When I open the board
      Then it is not shown

  Rule: A Phase set on the board is its row's state and its gate's date, on the release's branch

    Scenario: A move signs the gate
      Given the card of Project sync in automation
      When I set its Phase to verification
      Then the release's branch receives a commit with the row in Verification
      And the row's Spec green cell holds the date of the move, linked to the feature's automation section

    Scenario: A move to archive signs no gate
      Given the card of One name in verification
      When I set its Phase to archive
      Then the row reads Archive
      And In production stays the release's gate, signed in the verification file

    Scenario: The pull's commit starts no run
      Given a move recorded on the release's branch
      When its commit is pushed
      Then no run starts, and the issue's body says the new state

    Scenario: The sync's own write changes nothing
      Given a Phase written by the sync from the row's state
      When the pull runs
      Then it commits nothing

  Rule: A release's milestone names its branch

    Scenario: The sync writes the branch into the milestone
      Given the overview of 0.0.6 names the branch eventing-without-confluent
      When I sync the board
      Then the milestone 0.0.6 has the description eventing-without-confluent

    Scenario: A move in a release without a branch goes back
      Given the card of a feature of 0.0.7, whose overview names no branch
      When I set its Phase
      Then its Phase goes back to the row's state
      And the job summary names the card
