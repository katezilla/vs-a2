package client;

/**
* client/ClientHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from Client.idl
* Montag, 4. Mai 2015 21:53 Uhr MESZ
*/

public final class ClientHolder implements org.omg.CORBA.portable.Streamable
{
  public client.Client value = null;

  public ClientHolder ()
  {
  }

  public ClientHolder (client.Client initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = client.ClientHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    client.ClientHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return client.ClientHelper.type ();
  }

}
