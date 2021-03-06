package koordinator;


/**
* koordinator/KoordinatorOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from Koordinator.idl
* Montag, 4. Mai 2015 21:58 Uhr MESZ
*/

public interface KoordinatorOperations 
{
  void anmelden (int typId, String prozessId);
  void informieren (String prozessId, int sequenzNr, boolean termStatus, int letzteZahl);
  String[] getStarterIds ();
  void berechnen (String monitorId, int anzahlGgtLower, int anzahlGgtUpper, int delayZeitLower, int delayZeitUpper, int termAbfragePeriode, int gewuenschterGgt);
  void beenden (String prozessIdAbsender);
} // interface KoordinatorOperations
